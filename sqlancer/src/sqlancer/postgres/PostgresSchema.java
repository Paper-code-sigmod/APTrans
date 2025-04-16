package sqlancer.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PSQLException;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.DBMSCommon;
import sqlancer.common.schema.AbstractRelationalTable;
import sqlancer.common.schema.AbstractRowValue;
import sqlancer.common.schema.AbstractSchema;
import sqlancer.common.schema.AbstractTableColumn;
import sqlancer.common.schema.AbstractTables;
import sqlancer.common.schema.TableIndex;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLConstant.MySQLDoubleConstant;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresSchema.PostgresTable.TableType;
import sqlancer.postgres.ast.PostgresConstant;

public class PostgresSchema extends AbstractSchema<PostgresGlobalState, PostgresTable> {

//    private final String databaseName;

    public enum PostgresDataType {
        INT, BOOLEAN, TEXT, DECIMAL, FLOAT, REAL, RANGE, MONEY, BIT, INET;

        public static PostgresDataType getRandomType() {
            List<PostgresDataType> dataTypes = new ArrayList<>(Arrays.asList(values()));
            // todo: pg系列剔除了许多类型
            dataTypes.remove(PostgresDataType.INET);
            dataTypes.remove(PostgresDataType.RANGE);
            dataTypes.remove(PostgresDataType.MONEY);
            dataTypes.remove(PostgresDataType.BIT);
            return Randomly.fromList(dataTypes);
        }

        public PostgresConstant getRandomValue(PostgresGlobalState state){ // 未使用
            if(Randomly.getBooleanWithSmallProbability()){
                return PostgresConstant.createNullConstant();
            }
            switch (this) {
                case INT:
                    return PostgresConstant.createIntConstant((int) state.getRandomly().getInteger());
                case TEXT:
                    String string = state.getRandomly().getString().replace("[","").replace("]", "").replace("\\", "").replace("\n", "");
                    return PostgresConstant.createTextConstant(string);
                case BOOLEAN:
                    return PostgresConstant.createBooleanConstant(Randomly.getBoolean());
                case REAL:
                case FLOAT:
                case DECIMAL:
                    double val = state.getRandomly().getDouble();
                    return PostgresConstant.createDoubleConstant(val);
                default:
                    throw new AssertionError(this);
            }
        }

        public boolean isNumeric() {
            switch (this) {
                case INT:
                case REAL:
                case FLOAT:
                case DECIMAL:
                    return true;
                case BOOLEAN:
                case TEXT:
                    return false;
                default:
                    throw new AssertionError(this);
            }
        }

        public PostgresConstant getRandomValue(PostgresGlobalState state, boolean isUnsigned) {
            if(Randomly.getBooleanWithSmallProbability()){
                return PostgresConstant.createNullConstant();
            }
            int lowerBound = -20;
            int upperBound = 10000;
            switch (this) { //限制在-20000到20000之间
                case INT:
                    int k;
                    if (isUnsigned) {
                       k = state.getRandomly().getPositiveIntegerInt();
                       k = k % upperBound;
                    }else {
                       k = state.getRandomly().getInteger(lowerBound, upperBound);
                    }
                    return PostgresConstant.createIntConstant(k);
                case TEXT:
                    String string = state.getRandomly().getString().replace("[","").replace("]", "").replace("\\", "").replace("\n", "");
                    return PostgresConstant.createTextConstant(string);
                case BOOLEAN:
                    return PostgresConstant.createBooleanConstant(Randomly.getBoolean());
                case REAL:
                case FLOAT:
                case DECIMAL:
                   double val = state.getRandomly().getDouble(lowerBound, upperBound);
                    if (isUnsigned && val < 0) {
                        val *= -1;
                    }
                    return PostgresConstant.createDoubleConstant(val);
                default:
                    throw new AssertionError(this);
            }
        }
    }

    public static class PostgresColumn extends AbstractTableColumn<PostgresTable, PostgresDataType> {
        public boolean isPrimaryKey;
        private boolean isForeignKey;
        private ForeignKeyReference foreignKeyReference;
        public int precision;
        public boolean isUnsigned;
        public String trueType;
        List<ColumnOptions> columnOptions = new ArrayList<>();

        // todo:检查有哪些
        public enum ColumnOptions {
            PRIMARY_KEY, NULL_OR_NOT_NULL, UNIQUE, NULL, NOT_NULL,
        }

        public PostgresColumn(String name, PostgresDataType columnType) {
            super(name, null, columnType);
        }

        public PostgresColumn(String name, PostgresDataType columnType, boolean isPrimaryKey, int precision, boolean isUnsigned) {
            super(name, null, columnType);
            this.isPrimaryKey = isPrimaryKey;
            this.precision = precision;
            this.isUnsigned = isUnsigned;
            this.isForeignKey = false;
        }

        public PostgresColumn(String name, PostgresDataType columnType, Map<String, Object> columnOptions_Dict, int precision) {
            super(name, null, columnType);
            this.isPrimaryKey = (boolean) columnOptions_Dict.get("isPrimaryKey");
            this.isForeignKey = (boolean) columnOptions_Dict.get("isForeignKey");
            this.isUnsigned = (boolean) columnOptions_Dict.get("isUnsigned");
            this.trueType = (String) columnOptions_Dict.get("trueType");
            this.precision = precision;
            if (isForeignKey) {
                this.foreignKeyReference = new ForeignKeyReference((String) columnOptions_Dict.get("referencedTable"), (String) columnOptions_Dict.get("referencedColumn"));
            }
            Object columnOptions_Obj = columnOptions_Dict.get("ColumnOptions");
            List<?> columnOptions_Dict_List = (List<?>) columnOptions_Obj;
            for (Object o : columnOptions_Dict_List) {
                if (o instanceof ColumnOptions) {
                    this.columnOptions.add((ColumnOptions) o);
                }
            }
        }

        public String getTrueType() {
            return trueType;
        }

        public int getPrecision() {
            return precision;
        }

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public boolean isForeignKey() {
            return isForeignKey;
        } // 记录是否是外键，引用的哪个表的哪个列

        public boolean isUnique() {
            return (columnOptions.contains(ColumnOptions.UNIQUE) || isPrimaryKey);
        }

        public boolean isNotNull() {
            return columnOptions.contains(ColumnOptions.NOT_NULL);
        }

        public ForeignKeyReference getForeignKeyReference() {
            return foreignKeyReference;
        }

        public String getForeignKeyReferenceStr() {
            return foreignKeyReference.getReferencedTableName() + "." + foreignKeyReference.getReferencedColumnName();
        }

        public String getForeignKeyReferenceTable() {
            return foreignKeyReference.getReferencedTableName();
        }

        // ForeignKeyReference 类
        public static class ForeignKeyReference {
            private final String referencedTableName;
            private final String referencedColumnName;

            public ForeignKeyReference(String referencedTableName, String referencedColumnName) {
                this.referencedTableName = referencedTableName;
                this.referencedColumnName = referencedColumnName;
            }

            public String getReferencedTableName() {
                return referencedTableName;
            }

            public String getReferencedColumnName() {
                return referencedColumnName;
            }
        }

        public static PostgresColumn createDummy(String name) {
            return new PostgresColumn(name, PostgresDataType.INT);
        }

    }

    public static class PostgresTables extends AbstractTables<PostgresTable, PostgresColumn> {

        public PostgresTables(List<PostgresTable> tables) {
            super(tables);
        }

        public PostgresRowValue getRandomRowValue(SQLConnection con) throws SQLException {
            String randomRow = String.format("SELECT %s FROM %s ORDER BY RANDOM() LIMIT 1", columnNamesAsString(
                    c -> c.getTable().getName() + "." + c.getName() + " AS " + c.getTable().getName() + c.getName()),
                    // columnNamesAsString(c -> "typeof(" + c.getTable().getName() + "." +
                    // c.getName() + ")")
                    tableNamesAsString());
            Map<PostgresColumn, PostgresConstant> values = new HashMap<>();
            try (Statement s = con.createStatement()) {
                ResultSet randomRowValues = s.executeQuery(randomRow);
                if (!randomRowValues.next()) {
                    throw new AssertionError("could not find random row! " + randomRow + "\n");
                }
                for (int i = 0; i < getColumns().size(); i++) {
                    PostgresColumn column = getColumns().get(i);
                    int columnIndex = randomRowValues.findColumn(column.getTable().getName() + column.getName());
                    assert columnIndex == i + 1;
                    PostgresConstant constant;
                    if (randomRowValues.getString(columnIndex) == null) {
                        constant = PostgresConstant.createNullConstant();
                    } else {
                        switch (column.getType()) {
                        case INT:
                            constant = PostgresConstant.createIntConstant(randomRowValues.getLong(columnIndex));
                            break;
                        case BOOLEAN:
                            constant = PostgresConstant.createBooleanConstant(randomRowValues.getBoolean(columnIndex));
                            break;
                        case TEXT:
                            constant = PostgresConstant.createTextConstant(randomRowValues.getString(columnIndex));
                            break;
                        default:
                            throw new IgnoreMeException();
                        }
                    }
                    values.put(column, constant);
                }
                assert !randomRowValues.next();
                return new PostgresRowValue(this, values);
            } catch (PSQLException e) {
                throw new IgnoreMeException();
            }

        }

    }

    public static PostgresDataType getColumnType(String typeString) {
        switch (typeString) {
        case "smallint":
        case "integer":
        case "bigint":
            return PostgresDataType.INT;
        case "boolean":
            return PostgresDataType.BOOLEAN;
        case "text":
        case "character":
        case "character varying":
        case "name":
        case "regclass":
            return PostgresDataType.TEXT;
        case "numeric":
            return PostgresDataType.DECIMAL;
        case "double precision":
            return PostgresDataType.FLOAT;
        case "real":
            return PostgresDataType.REAL;
        case "int4range":
            return PostgresDataType.RANGE;
        case "money":
            return PostgresDataType.MONEY;
        case "bit":
        case "bit varying":
            return PostgresDataType.BIT;
        case "inet":
            return PostgresDataType.INET;
        default:
            throw new AssertionError(typeString);
        }
    }

    public static class PostgresRowValue extends AbstractRowValue<PostgresTables, PostgresColumn, PostgresConstant> {

        protected PostgresRowValue(PostgresTables tables, Map<PostgresColumn, PostgresConstant> values) {
            super(tables, values);
        }

    }

    public static class PostgresTable
            extends AbstractRelationalTable<PostgresColumn, PostgresIndex, PostgresGlobalState> {

        public enum TableType {
            STANDARD, TEMPORARY
        }

        private final TableType tableType;
        private final List<PostgresStatisticsObject> statistics;
        private final boolean isInsertable;

        public PostgresTable(String tableName, List<PostgresColumn> columns, List<PostgresIndex> indexes,
                TableType tableType, List<PostgresStatisticsObject> statistics, boolean isView, boolean isInsertable) {
            super(tableName, columns, indexes, isView);
            this.statistics = statistics;
            this.isInsertable = isInsertable;
            this.tableType = tableType;
        }

        public List<PostgresStatisticsObject> getStatistics() {
            return statistics;
        }

        public TableType getTableType() {
            return tableType;
        }

        public boolean isInsertable() {
            return isInsertable;
        }

    }

    public static final class PostgresStatisticsObject {
        private final String name;

        public PostgresStatisticsObject(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final class PostgresIndex extends TableIndex {

        private PostgresIndex(String indexName) {
            super(indexName);
        }

        public static PostgresIndex create(String indexName) {
            return new PostgresIndex(indexName);
        }

        @Override
        public String getIndexName() {
            if (super.getIndexName().contentEquals("PRIMARY")) {
                return "`PRIMARY`";
            } else {
                return super.getIndexName();
            }
        }

    }

    public static PostgresSchema fromConnection(SQLConnection con, String databaseName) throws SQLException {
        try {
            List<PostgresTable> databaseTables = new ArrayList<>();
            try (Statement s = con.createStatement()) {
                try (ResultSet rs = s.executeQuery(
                        "SELECT table_name, table_schema, table_type, is_insertable_into FROM information_schema.tables WHERE table_schema='public' OR table_schema LIKE 'pg_temp_%' ORDER BY table_name;")) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        String tableTypeSchema = rs.getString("table_schema");
                        boolean isInsertable = rs.getBoolean("is_insertable_into");
                        // TODO: also check insertable
                        // TODO: insert into view?
                        boolean isView = tableName.startsWith("v");
                        PostgresTable.TableType tableType = getTableType(tableTypeSchema);
                        List<PostgresColumn> databaseColumns = getTableColumns(con, tableName);
                        List<PostgresIndex> indexes = getIndexes(con, tableName);
                        List<PostgresStatisticsObject> statistics = getStatistics(con);
                        PostgresTable t = new PostgresTable(tableName, databaseColumns, indexes, tableType, statistics,
                                isView, isInsertable);
                        for (PostgresColumn c : databaseColumns) {
                            c.setTable(t);
                        }
                        databaseTables.add(t);
                    }
                }
            }
            return new PostgresSchema(databaseTables);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new AssertionError(e);
        }
    }

    protected static List<PostgresStatisticsObject> getStatistics(SQLConnection con) throws SQLException {
        List<PostgresStatisticsObject> statistics = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT stxname FROM pg_statistic_ext ORDER BY stxname;")) {
                while (rs.next()) {
                    statistics.add(new PostgresStatisticsObject(rs.getString("stxname")));
                }
            }
        }
        return statistics;
    }

    protected static PostgresTable.TableType getTableType(String tableTypeStr) throws AssertionError {
        PostgresTable.TableType tableType;
        if (tableTypeStr.contentEquals("public")) {
            tableType = TableType.STANDARD;
        } else if (tableTypeStr.startsWith("pg_temp")) {
            tableType = TableType.TEMPORARY;
        } else {
            throw new AssertionError(tableTypeStr);
        }
        return tableType;
    }

    protected static List<PostgresIndex> getIndexes(SQLConnection con, String tableName) throws SQLException {
        List<PostgresIndex> indexes = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(String
                    .format("SELECT indexname FROM pg_indexes WHERE tablename='%s' ORDER BY indexname;", tableName))) {
                while (rs.next()) {
                    String indexName = rs.getString("indexname");
                    if (DBMSCommon.matchesIndexName(indexName)) {
                        indexes.add(PostgresIndex.create(indexName));
                    }
                }
            }
        }
        return indexes;
    }

    protected static List<PostgresColumn> getTableColumns(SQLConnection con, String tableName) throws SQLException {
        List<PostgresColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s
                    .executeQuery("select column_name, data_type from INFORMATION_SCHEMA.COLUMNS where table_name = '"
                            + tableName + "' ORDER BY column_name")) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    PostgresColumn c = new PostgresColumn(columnName, getColumnType(dataType));
                    columns.add(c);
                }
            }
        }
        return columns;
    }

    public PostgresSchema(List<PostgresTable> databaseTables) {
        super(databaseTables);
    }

    public PostgresTables getRandomTableNonEmptyTables() {
        return new PostgresTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

}
