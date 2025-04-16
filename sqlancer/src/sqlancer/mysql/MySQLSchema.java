package sqlancer.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Stream;

import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.schema.AbstractRelationalTable;
import sqlancer.common.schema.AbstractRowValue;
import sqlancer.common.schema.AbstractSchema;
import sqlancer.common.schema.AbstractTableColumn;
import sqlancer.common.schema.AbstractTables;
import sqlancer.common.schema.TableIndex;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.MySQLSchema.MySQLTable.MySQLEngine;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLConstant.MySQLDoubleConstant;

public class MySQLSchema extends AbstractSchema<MySQLGlobalState, MySQLTable> {

    private static final int NR_SCHEMA_READ_TRIES = 10;
    // private static List<MySQLTable> tables =  new ArrayList<>();

    public enum MySQLDataType {
        INT, VARCHAR, FLOAT, DOUBLE, DECIMAL, BOOLEAN;

        public static MySQLDataType getRandom() {
            return Randomly.fromOptions(values());
        }

        public boolean isNumeric() {
            switch (this) {
            case INT:
            case DOUBLE:
            case FLOAT:
            case DECIMAL:
                return true;
            case BOOLEAN:
            case VARCHAR:
                return false;
            default:
                throw new AssertionError(this);
            }
        }

        public boolean isString() {
            switch (this) {
            case INT:
            case DOUBLE:
            case FLOAT:
            case DECIMAL:
            case BOOLEAN:
                return false;
            case VARCHAR:
                return true;
            default:
                throw new AssertionError(this);
            }
        }

        public MySQLConstant getRandomValue(MySQLGlobalState state){
            if(Randomly.getBooleanWithSmallProbability()){
                return MySQLConstant.createNullConstant();
            }
            switch (this) {
                case INT:
                    return MySQLConstant.createIntConstant((int) state.getRandomly().getInteger());
                case VARCHAR:
                    String string = state.getRandomly().getString().replace("[","").replace("]", "").replace("\\", "").replace("\n", "");
                    return MySQLConstant.createStringConstant(string);
                case BOOLEAN:
                    return MySQLConstant.createBoolean(Randomly.getBoolean());
                case DOUBLE:
                case FLOAT:
                case DECIMAL:
                    double val = state.getRandomly().getDouble();
                    return new MySQLDoubleConstant(val);
                default:
                    throw new AssertionError(this);
            }
        }

        public MySQLConstant getRandomValue(MySQLGlobalState state, boolean isUnsigned){
            if(Randomly.getBooleanWithSmallProbability()){
                return MySQLConstant.createNullConstant(); // 有小概率返回NULL
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
                    return MySQLConstant.createIntConstant(k);
                case VARCHAR:
                    String string = state.getRandomly().getString().replace("[","").replace("]", "").replace("\\", "").replace("\n", "");
                    return MySQLConstant.createStringConstant(string);
                case BOOLEAN:
                    return MySQLConstant.createBoolean(Randomly.getBoolean());
                case DOUBLE:
                case FLOAT:
                case DECIMAL:
                   double val = state.getRandomly().getDouble(lowerBound, upperBound);
                    if (isUnsigned && val < 0) {
                        val *= -1;
                    }
                    return new MySQLDoubleConstant(val);
                default:
                    throw new AssertionError(this);
            }
        }

    }

    public static class MySQLColumn extends AbstractTableColumn<MySQLTable, MySQLDataType> {

        private final boolean isPrimaryKey;
        private final boolean isForeignKey;
        private ForeignKeyReference foreignKeyReference;
        private final int precision;
        public boolean isUnsigned;
        public String trueType;
        List<ColumnOptions> columnOptions = new ArrayList<>();
//        List<>
        public String getTrueType() {
            return trueType;
        }

        public List<ColumnOptions> getColumnOptions() {
            return columnOptions;
        }

        public enum ColumnOptions {
            PRIMARY_KEY, NULL_OR_NOT_NULL, UNIQUE_KEY, UNIQUE, NULL, NOT_NULL,
        }

//        public enum CollateSequence {
//            NOCASE, RTRIM, BINARY;
//            public static CollateSequence random() {
//                return Randomly.fromOptions(values());
//            }
//        }

        public MySQLColumn(String name, MySQLDataType columnType, boolean isPrimaryKey, int precision) { //未使用
            super(name, null, columnType);
            this.isPrimaryKey = isPrimaryKey;
            this.isForeignKey = false; // 默认不是外键
            this.precision = precision;
        }

        public MySQLColumn(String name, MySQLDataType columnType, boolean isPrimaryKey, int precision, boolean isUnsigned) { // 未使用
            super(name, null, columnType);
            this.isPrimaryKey = isPrimaryKey;
            this.isForeignKey = false; // 默认不是外键
            this.precision = precision;
            this.isUnsigned = isUnsigned;
        }

        public MySQLColumn(String name, MySQLDataType columnType, Map<String, Object> columnOptions_Dict, int precision) {
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

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public boolean isForeignKey() {
            return isForeignKey;
        } // 记录是否是外键，引用的哪个表的哪个列

        public boolean isUnique() {
            return (columnOptions.contains(ColumnOptions.UNIQUE) || columnOptions.contains(ColumnOptions.UNIQUE_KEY) || isPrimaryKey);
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

    }

    public static class MySQLTables extends AbstractTables<MySQLTable, MySQLColumn> {

        public MySQLTables(List<MySQLTable> tables) {
            super(tables);
        }

        public MySQLRowValue getRandomRowValue(SQLConnection con) throws SQLException {
            String randomRow = String.format("SELECT %s FROM %s ORDER BY RAND() LIMIT 1", columnNamesAsString(
                    c -> c.getTable().getName() + "." + c.getName() + " AS " + c.getTable().getName() + c.getName()),
                    // columnNamesAsString(c -> "typeof(" + c.getTable().getName() + "." +
                    // c.getName() + ")")
                    tableNamesAsString());
            Map<MySQLColumn, MySQLConstant> values = new HashMap<>();
            try (Statement s = con.createStatement()) {
                ResultSet randomRowValues = s.executeQuery(randomRow);
                if (!randomRowValues.next()) {
                    throw new AssertionError("could not find random row! " + randomRow + "\n");
                }
                for (int i = 0; i < getColumns().size(); i++) {
                    MySQLColumn column = getColumns().get(i);
                    Object value;
                    int columnIndex = randomRowValues.findColumn(column.getTable().getName() + column.getName());
                    assert columnIndex == i + 1;
                    MySQLConstant constant;
                    if (randomRowValues.getString(columnIndex) == null) {
                        constant = MySQLConstant.createNullConstant();
                    } else {
                        switch (column.getType()) {
                        case INT:
                            value = randomRowValues.getLong(columnIndex);
                            constant = MySQLConstant.createIntConstant((long) value);
                            break;
                        case VARCHAR:
                            value = randomRowValues.getString(columnIndex);
                            constant = MySQLConstant.createStringConstant((String) value);
                            break;
                        default:
                            throw new AssertionError(column.getType());
                        }
                    }
                    values.put(column, constant);
                }
                assert !randomRowValues.next();
                return new MySQLRowValue(this, values);
            }
        }
    }

    private static MySQLDataType getColumnType(String typeString) {
        switch (typeString) {
        case "tinyint":
        case "smallint":
        case "mediumint":
        case "int":
        case "bigint":
            return MySQLDataType.INT;
        case "varchar":
        case "tinytext":
        case "mediumtext":
        case "text":
        case "longtext":
            return MySQLDataType.VARCHAR;
        case "double":
            return MySQLDataType.DOUBLE;
        case "float":
            return MySQLDataType.FLOAT;
        case "decimal":
            return MySQLDataType.DECIMAL;
        default:
            throw new AssertionError(typeString);
        }
    }

    public static class MySQLRowValue extends AbstractRowValue<MySQLTables, MySQLColumn, MySQLConstant> {

        MySQLRowValue(MySQLTables tables, Map<MySQLColumn, MySQLConstant> values) {
            super(tables, values);
        }

    }

    public static class MySQLTable extends AbstractRelationalTable<MySQLColumn, MySQLIndex, MySQLGlobalState> {

        public enum MySQLEngine {
            INNO_DB("InnoDB"), MY_ISAM("MyISAM"), MEMORY("MEMORY"), HEAP("HEAP"), CSV("CSV"), MERGE("MERGE"),
            ARCHIVE("ARCHIVE"), FEDERATED("FEDERATED");

            private String s;

            MySQLEngine(String s) {
                this.s = s;
            }

            public static MySQLEngine get(String val) {
                return Stream.of(values()).filter(engine -> engine.s.equalsIgnoreCase(val)).findFirst().get();
            }

        }

        private final MySQLEngine engine;

        public MySQLTable(String tableName, List<MySQLColumn> columns, List<MySQLIndex> indexes, MySQLEngine engine) {
            super(tableName, columns, indexes, false /* TODO: support views */);
            this.engine = engine;
        }

        public MySQLEngine getEngine() {
            return engine;
        }

        public boolean hasPrimaryKey() {
            return getColumns().stream().anyMatch(c -> c.isPrimaryKey());
        }

    }

    public static final class MySQLIndex extends TableIndex {

        private MySQLIndex(String indexName) {
            super(indexName);
        }

        public static MySQLIndex create(String indexName) {
            return new MySQLIndex(indexName);
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

    public static MySQLSchema fromConnection(SQLConnection con, String databaseName) throws SQLException {
        Exception ex = null;
        /* the loop is a workaround for https://bugs.mysql.com/bug.php?id=95929 */
        for (int i = 0; i < NR_SCHEMA_READ_TRIES; i++) {
            try {
                List<MySQLTable> databaseTables = new ArrayList<>();
                try (Statement s = con.createStatement()) {
                    try (ResultSet rs = s.executeQuery("select TABLE_NAME, ENGINE from information_schema.TABLES where table_schema = '" + databaseName + "';")) {
                        while (rs.next()) {
                            String tableName = rs.getString("TABLE_NAME");
                            String tableEngineStr = rs.getString("ENGINE");
                            MySQLEngine engine = MySQLEngine.get(tableEngineStr);
                            List<MySQLColumn> databaseColumns = getTableColumns(con, tableName, databaseName);
                            List<MySQLIndex> indexes = getIndexes(con, tableName, databaseName);
                            MySQLTable t = new MySQLTable(tableName, databaseColumns, indexes, engine);
                            for (MySQLColumn c : databaseColumns) {
                                c.setTable(t);
                            }
                            databaseTables.add(t);
                        }
                    }
                }
                return new MySQLSchema(databaseTables);
            } catch (SQLIntegrityConstraintViolationException e) {
                ex = e;
            }
        }
        throw new AssertionError(ex);
    }

    private static List<MySQLIndex> getIndexes(SQLConnection con, String tableName, String databaseName)
            throws SQLException {
        List<MySQLIndex> indexes = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(String.format(
                    "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME='%s';",
                    databaseName, tableName))) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    indexes.add(MySQLIndex.create(indexName));
                }
            }
        }
        return indexes;
    }

    private static List<MySQLColumn> getTableColumns(SQLConnection con, String tableName, String databaseName)
            throws SQLException {
        List<MySQLColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery("select * from information_schema.columns where table_schema = '"
                    + databaseName + "' AND TABLE_NAME='" + tableName + "'")) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    int precision = rs.getInt("NUMERIC_PRECISION");
                    boolean isPrimaryKey = rs.getString("COLUMN_KEY").equals("PRI");
                    MySQLColumn c = new MySQLColumn(columnName, getColumnType(dataType), isPrimaryKey, precision);
                    columns.add(c);
                }
            }
        }
        return columns;
    }

    public MySQLSchema(List<MySQLTable> databaseTables) {
        super(databaseTables);
    }

    public MySQLTables getRandomTableNonEmptyTables() { // 获得多张非空表
        return new MySQLTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

    public MySQLTables getRandomTableNonEmptyTables(int nrTables) { // 获得指定数量的多张非空表
        return new MySQLTables(Randomly.nonEmptySubset(getDatabaseTables(), nrTables));
    }

}
