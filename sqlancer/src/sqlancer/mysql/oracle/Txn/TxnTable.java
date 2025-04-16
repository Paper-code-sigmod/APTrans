package sqlancer.mysql.oracle.Txn;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.oracle.MySQLTxnGen;

public class TxnTable {
    private String tableName;
    private List<MySQLColumn> columns;
    private MySQLColumn primaryKey;
    private String createSql;
    private final boolean allowPrimaryKey;
    private boolean setPrimaryKey;
    private boolean setUniqueKey;
    private final StringBuilder sb = new StringBuilder();
    private final MySQLGlobalState globalState;
    private MySQLTable sqlTable;

    public TxnTable(){
        this.allowPrimaryKey = Randomly.getBoolean();
        this.globalState = null;
        this.columns = new ArrayList<>();
    }

    public TxnTable(MySQLGlobalState globalState) {
        this.allowPrimaryKey = Randomly.getBoolean();
        this.globalState = globalState;
        this.columns = new ArrayList<>();
        genRandomTable();
    }

    public boolean isSetUniqueKey() {
        return setPrimaryKey || setUniqueKey;
    }

    public boolean isSetPrimaryKey() {
        return setPrimaryKey;
    }

    public String getTableName() {
        return tableName;
    }

    public List<MySQLColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<MySQLColumn> columns) {
        this.columns = columns;
    }

    public String getCreateSql() {
        return createSql;
    }


    public MySQLColumn getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table Name: ").append(tableName).append("\n");
        sb.append("Columns: ").append(columns).append("\n");
        sb.append("Create SQL: ").append(createSql).append("\n");
        return sb.toString();
    }

    public MySQLTable getMySQLTable() {
        return sqlTable;
    }

    private void genRandomTable() {
        tableName = "t" + RandomStringUtils.randomAlphanumeric(7);
        sb.append("CREATE TABLE ").append(tableName).append(" (");
        appendIDColumn();
        for (int i = 0; i < 3; i++) {
            if (i > 0)
                sb.append(", ");
            appendColumn(i);
        }
        sqlTable = new MySQLTable(tableName, columns, null, null);

        for (MySQLColumn column : columns) {
            column.setTable(sqlTable);
            if (column.isForeignKey()) {
                MySQLColumn.ForeignKeyReference foreignKeyReference = column.getForeignKeyReference();
                sb.append(", FOREIGN KEY(");
                sb.append(column.getName());
                sb.append(") REFERENCES ");
                sb.append(foreignKeyReference.getReferencedTableName());
                sb.append("(");
                sb.append(foreignKeyReference.getReferencedColumnName());
                sb.append(")");
                MySQLTxnGen.foreign_key_count++;
            }
        }
        sb.append(");");
        createSql = sb.toString();
    }

    private void appendIDColumn() {
        sb.append("ID INT, VAL INT, ");
    }

    private void appendColumn(int columnId) {
        String columnName = DBMSCommon.createColumnName(columnId);
        sb.append(columnName).append(" ");
        Map<String, Object> columnOption_Dict = new HashMap<>();
        MySQLDataType randomType = MySQLDataType.getRandom();
        boolean isUnsigned = false;
        if (Randomly.getBoolean() && randomType == MySQLDataType.INT)
            isUnsigned = true;
        String True_Type = appendType(randomType, isUnsigned);
        sb.append(" ");
        columnOption_Dict.put("isUnsigned", isUnsigned);
        columnOption_Dict.put("trueType", True_Type);
        columnOption_Dict.putAll(appendColumnOption(randomType));
        boolean isUnique = columnOption_Dict.get("ColumnOptions").toString().contains("UNIQUE_KEY") || columnOption_Dict.get("ColumnOptions").toString().contains("UNIQUE") || columnOption_Dict.get("ColumnOptions").toString().contains("PRIMARY_KEY");
        columnOption_Dict.putAll(appendForeignKey(True_Type, isUnique));
        MySQLColumn column = new MySQLColumn(columnName, randomType, columnOption_Dict, 4);
        if (column.isPrimaryKey()) {
            primaryKey = column;
        }
        columns.add(column);
    }

    private Map<String, Object> appendForeignKey(String randomType, boolean isUnique) {
        if (isUnique || Randomly.getBoolean() || randomType.contains("BOOLEAN")) {
            return Map.of("isForeignKey", false, "referencedTable", new Object(), "referencedColumn", new Object());
        }
        List<MySQLTable> tables = globalState.getSchema().getDatabaseTables();
        if (!tables.isEmpty() && Randomly.getBoolean()) {
            MySQLTable candidate_table = Randomly.fromList(tables);
            List<MySQLColumn> candidate_columns = candidate_table.getColumns();
            candidate_columns = candidate_columns.stream().filter(c -> c.getTrueType().equals(randomType)).collect(Collectors.toList());
            candidate_columns = candidate_columns.stream().filter(MySQLColumn::isUnique).collect(Collectors.toList());
            if (!candidate_columns.isEmpty()) {
                MySQLColumn select_column = Randomly.fromList(candidate_columns);
                return Map.of("isForeignKey", true, "referencedTable", candidate_table.getName(), "referencedColumn", select_column.getName());
            }
        }
        return Map.of("isForeignKey", false, "referencedTable", new Object(), "referencedColumn", new Object());
    }

    private String appendType(MySQLDataType randomType, boolean isUnsigned) {
        String True_Type = "";
        switch (randomType) {
            case DECIMAL:
                True_Type = "DECIMAL";
                StringBuilder sb1 = new StringBuilder();
                optionallyAddPrecisionAndScale(sb1, true);
                True_Type = True_Type + sb1.toString();
                break;
            case INT:
                True_Type = Randomly.fromOptions("INT", "BIGINT");
                break;
            case VARCHAR:
                True_Type = Randomly.fromOptions("VARCHAR(100)", "TEXT");
                break;
            case FLOAT:
                // True_Type = "FLOAT";
                // break;
            case DOUBLE:
                True_Type = "DOUBLE";
                break;
            case BOOLEAN:
                True_Type = "BOOLEAN";
                break;
            default:
                throw new AssertionError();
        }
        if (randomType.isNumeric() && isUnsigned) {
            True_Type = True_Type + " UNSIGNED";
        }
        sb.append(True_Type);
        return True_Type;
    }

    private Map<String, Object> appendColumnOption(MySQLDataType type) {
        if (type == MySQLDataType.BOOLEAN){
            return Map.of("isPrimaryKey", false, "ColumnOptions", new ArrayList<>());
        }
        Map<String, Object> Settings_Map = new HashMap<>();
        boolean isTextType = type == MySQLDataType.VARCHAR;
        boolean isNull = false;
        boolean columnHasPrimaryKey = false;
        List<MySQLColumn.ColumnOptions> columnOptions = Randomly.subset(MySQLColumn.ColumnOptions.values());
        List<MySQLColumn.ColumnOptions> true_columnOptions = new ArrayList<>();
        columnOptions.remove(MySQLColumn.ColumnOptions.NOT_NULL);
        columnOptions.remove(MySQLColumn.ColumnOptions.NULL);
        columnOptions.remove(MySQLColumn.ColumnOptions.UNIQUE_KEY);
        if (isTextType) {
            columnOptions.remove(MySQLColumn.ColumnOptions.PRIMARY_KEY);
            columnOptions.remove(MySQLColumn.ColumnOptions.UNIQUE);
        }
        for (MySQLColumn.ColumnOptions o : columnOptions) {
            sb.append(" ");
            switch (o) {
            case NULL_OR_NOT_NULL:
                if (!columnHasPrimaryKey) {
                    if (Randomly.getBoolean() && !setPrimaryKey) {
                        sb.append("NULL");
                        true_columnOptions.add(MySQLColumn.ColumnOptions.NULL);
                    }
                    isNull = true;
                } else {
                    sb.append("NOT NULL");
                    true_columnOptions.add(MySQLColumn.ColumnOptions.NOT_NULL);
                }
                break;
            case UNIQUE:
                if (Randomly.getBoolean() && !setPrimaryKey) {
                    sb.append("UNIQUE KEY");
                    true_columnOptions.add(MySQLColumn.ColumnOptions.UNIQUE_KEY);
                } else {
                    if (!setPrimaryKey){
                        sb.append("UNIQUE");
                        true_columnOptions.add(MySQLColumn.ColumnOptions.UNIQUE);
                    }
                }
                setUniqueKey = true;
                break;
            case PRIMARY_KEY:
                if (allowPrimaryKey && !setPrimaryKey && !isNull) {
                    sb.append("PRIMARY KEY");
                    setPrimaryKey = true;
                    columnHasPrimaryKey = true;
                    true_columnOptions.add(MySQLColumn.ColumnOptions.PRIMARY_KEY);
                }
                break;
            default:
                throw new AssertionError();
            }
        }
        Settings_Map.put("isPrimaryKey", columnHasPrimaryKey);
        Settings_Map.put("ColumnOptions", true_columnOptions);
        return Settings_Map;
    }

    private void optionallyAddPrecisionAndScale(StringBuilder sb, boolean isDECIMAL) {
        if (isDECIMAL || Randomly.getBoolean()) {
            sb.append("(");
            long nCandidate = Randomly.getNotCachedInteger(2, 30);
            long n = Math.min(nCandidate, 32);
            long m = Randomly.getNotCachedInteger((int) n + 10, 65);
            sb.append(m);
            sb.append(", ");
            sb.append(n);
            sb.append(")");
        }
    }
}
