package sqlancer.postgres.oracle.Txn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.oracle.PostgresTxnGen;

public class TxnTable {
    private String tableName;
    private List<PostgresColumn> columns;
    private String createSql;
    private final boolean allowPrimaryKey;
    private boolean setPrimaryKey;
    private boolean setUniqueKey; 
    private final StringBuilder sb = new StringBuilder();
    private final PostgresGlobalState globalState;
    private PostgresTable sqlTable;

    public TxnTable() {
        this.allowPrimaryKey = false;
        this.columns = null;
        this.globalState = null;
    }

    public TxnTable(PostgresGlobalState globalState) {
        this.allowPrimaryKey = Randomly.getBoolean();
        this.columns = new ArrayList<>();
        this.globalState = globalState;
        genRandomTable();
    }

    public boolean isSetUniqueKey() {
        return setPrimaryKey || setUniqueKey;
    }

    public String getTableName() {
        return tableName;
    }

    public List<PostgresColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<PostgresColumn> columns) {
        this.columns = columns;
    }

    public String getCreateSql() {
        return createSql;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table Name: ").append(tableName).append("\n");
        sb.append("Columns: ").append(columns).append("\n");
        sb.append("Create SQL: ").append(createSql).append("\n");
        return sb.toString();
    }

    public PostgresTable getPostgresTable() {
        return sqlTable;
    }

    private void genRandomTable() {
        tableName = "t" + RandomStringUtils.randomAlphanumeric(7);
        sb.append("CREATE TABLE ").append(tableName).append(" (");
        appendIDColumn(); 
        for (int i = 0; i < 3; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            appendColumn(i);
        }
        sqlTable = new PostgresTable(tableName, columns, null, null, null, false, false);

        for (PostgresColumn column : columns) {
            column.setTable(sqlTable);
            if (column.isForeignKey()) {
                PostgresColumn.ForeignKeyReference foreignKeyReference = column.getForeignKeyReference();
                sb.append(", FOREIGN KEY(");
                sb.append(column.getName());
                sb.append(") REFERENCES ");
                sb.append(foreignKeyReference.getReferencedTableName());
                sb.append("(");
                sb.append(foreignKeyReference.getReferencedColumnName());
                sb.append(")");
                PostgresTxnGen.foreign_key_count++;
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
        PostgresDataType randomType = PostgresDataType.getRandomType();
        boolean isUnsigned = false;
        if (Randomly.getBooleanWithSmallProbability() && randomType.isNumeric())
            isUnsigned = true;
        String True_Type = appendType(randomType, isUnsigned);
        sb.append(" ");
        columnOption_Dict.put("isUnsigned", isUnsigned);
        columnOption_Dict.put("trueType", True_Type);
        columnOption_Dict.putAll(appendColumnOption(randomType)); 
        boolean isUnique = columnOption_Dict.get("ColumnOptions").toString().contains("UNIQUE_KEY") || columnOption_Dict.get("ColumnOptions").toString().contains("UNIQUE") || columnOption_Dict.get("ColumnOptions").toString().contains("PRIMARY_KEY");
        columnOption_Dict.putAll(appendForeignKey(True_Type, isUnique)); 
        PostgresColumn column = new PostgresColumn(columnName, randomType, columnOption_Dict, 4);
        columns.add(column);
    }

    private Map<String, Object> appendForeignKey(String randomType, boolean isUnique) {
        if (isUnique || Randomly.getBoolean() || randomType.contains("BOOLEAN")) { 
            return Map.of("isForeignKey", false, "referencedTable", new Object(), "referencedColumn", new Object());
        }
        List<PostgresTable> tables = globalState.getSchema().getDatabaseTables();
        if (!tables.isEmpty() && Randomly.getBoolean()) { 
            PostgresTable candidate_table = Randomly.fromList(tables); 
            List<PostgresColumn> candidate_columns = candidate_table.getColumns();
            candidate_columns = candidate_columns.stream().filter(c -> c.getTrueType().equals(randomType)).collect(Collectors.toList());
            candidate_columns = candidate_columns.stream().filter(c -> c.isPrimaryKey() || c.isUnique()).collect(Collectors.toList());
            if (!candidate_columns.isEmpty()) { 
                PostgresColumn select_column = Randomly.fromList(candidate_columns); 
                return Map.of("isForeignKey", true, "referencedTable", candidate_table.getName(), "referencedColumn", select_column.getName());
            }
        }
//        else {
//            List<PostgresColumn> candidate_columns = columns.stream().filter(c -> c.getTrueType().equals(randomType)).collect(Collectors.toList());
//            candidate_columns = candidate_columns.stream().filter(c -> c.isPrimaryKey() || c.isUnique()).collect(Collectors.toList());
//            if (!candidate_columns.isEmpty()) {
//                PostgresColumn select_column = Randomly.fromList(candidate_columns);
//                return Map.of("isForeignKey", true, "referencedTable", tableName, "referencedColumn", select_column.getName());
//            }
//        }
        return Map.of("isForeignKey", false, "referencedTable", new Object(), "referencedColumn", new Object());
    }

    private String appendType(PostgresDataType randomType, boolean isUnsigned) {
        String True_Type = ""; 
        switch (randomType) {
        case DECIMAL:
            True_Type = "DECIMAL";
            StringBuilder sb1 = new StringBuilder(); 
            optionallyAddPrecisionAndScale(sb1, true);
            True_Type = True_Type + sb1.toString();
            break;
        case INT:
            True_Type = "INT";
            break;
        case TEXT:
            True_Type = "TEXT";
            break;
        case FLOAT:
            // True_Type = "FLOAT";
            // break;
        case REAL:
            True_Type = "REAL";
            break;
        case BOOLEAN:
            True_Type = "BOOLEAN";
            break;
        default:
            throw new AssertionError();
        }
        // if (randomType.isNumeric() && isUnsigned) {
        //     True_Type = True_Type + " UNSIGNED";
        // }
        sb.append(True_Type);
        return True_Type;
    }

    private Map<String, Object> appendColumnOption(PostgresDataType type) {
        if (type == PostgresDataType.BOOLEAN){
            return Map.of("isPrimaryKey", false, "ColumnOptions", new ArrayList<>());
        }
        Map<String, Object> Settings_Map = new HashMap<>(); 
        boolean isTextType = type == PostgresDataType.TEXT;
        boolean isNull = false;
        boolean columnHasPrimaryKey = false;
        List<PostgresColumn.ColumnOptions> columnOptions = Randomly.subset(PostgresColumn.ColumnOptions.values());
        List<PostgresColumn.ColumnOptions> true_columnOptions = new ArrayList<>(); 
        columnOptions.remove(PostgresColumn.ColumnOptions.NOT_NULL); 
        columnOptions.remove(PostgresColumn.ColumnOptions.NULL);
        if (isTextType) {
            columnOptions.remove(PostgresColumn.ColumnOptions.PRIMARY_KEY);
            columnOptions.remove(PostgresColumn.ColumnOptions.UNIQUE);
        }
        for (PostgresColumn.ColumnOptions o : columnOptions) {
            sb.append(" ");
            switch (o) {
            case NULL_OR_NOT_NULL:
                if (!columnHasPrimaryKey) {
                    if (Randomly.getBoolean()) {
                        sb.append("NULL");
                        true_columnOptions.add(PostgresColumn.ColumnOptions.NULL);
                    }
                    isNull = true;
                } else {
                    sb.append("NOT NULL");
                    true_columnOptions.add(PostgresColumn.ColumnOptions.NOT_NULL);
                }
                break;
            case UNIQUE:
                if (!columnHasPrimaryKey) {
                    sb.append("UNIQUE"); 
                    true_columnOptions.add(PostgresColumn.ColumnOptions.UNIQUE);
                }
                setUniqueKey = true;
                break;
            case PRIMARY_KEY:
                if (allowPrimaryKey && !setPrimaryKey && !isNull) { 
                    sb.append("PRIMARY KEY");
                    setPrimaryKey = true;
                    columnHasPrimaryKey = true;
                    true_columnOptions.add(PostgresColumn.ColumnOptions.PRIMARY_KEY);
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
