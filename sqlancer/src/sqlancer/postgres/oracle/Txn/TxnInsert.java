package sqlancer.postgres.oracle.Txn;

import java.util.*;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.Txn_constant;

public class TxnInsert {
    private String insertSql;
    private final PostgresGlobalState globalState;

    public TxnInsert(PostgresGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public String toString() {
        return insertSql;
    }

    public String genInsertStatement(Condition condition, Map<String, List<String>> Data){
        StringBuilder sb = new StringBuilder();
        PostgresTable table = condition.getFetchTable().getPostgresTable();
        List<PostgresColumn> columns = table.getColumns();
        Map<String, List<String>> result = new HashMap<>();
        List<String> columnNames = new ArrayList<>();
        int values_length = new Random().nextInt(2) + 1;

        for (PostgresColumn column : columns) {
            List<String> values_list = new ArrayList<>();
            String table_column_name = table.getName() + "." + column.getName();
            String column_name = column.getName();
            if (Randomly.getBooleanWithSmallProbability() && !column.isUnique() && !column.isNotNull() && !columnNames.isEmpty()) { 
                for (int i = 0; i < values_length; i++) {
                    values_list.add("NULL");
                }
                columnNames.add(column_name);
                result.put(column_name, values_list);
                continue;
            }
            PostgresDataType dataType = column.getType();
            if (column.isForeignKey()) { 
                String ref_table_column_name = column.getForeignKeyReferenceStr();
                for (int i = 0; i < values_length; i++) { 
                    String val;
                    if (dataType.isNumeric()){
                        List<String> foreignData = Data.get(ref_table_column_name).stream().filter(x -> Double.parseDouble(x) < 100).collect(Collectors.toList()); 
                        val = foreignData.get(foreignData.size() - 1);
                    } else {
                        val = Data.get(ref_table_column_name).get(new Random().nextInt(Data.get(ref_table_column_name).size()));
                    }
                    values_list.add(val);
                }
            } else if (column.isUnique()) { 
                for (int i = 0; i < values_length; i++) {
                    String val = "NULL";
                    while (val.equals("NULL") || Data.get(table_column_name).contains(val)) {
                        val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
                    }
                    Data.get(table_column_name).add(val);
                    values_list.add(val);
                }
            } else if (column.isNotNull()) { 
                for (int i = 0; i < values_length; i++) {
                    String val = "NULL";
                    while (val.equals("NULL")) {
                        val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
                    }
                    values_list.add(val);
                }
            } else { 
                for (int i = 0; i < values_length; i++) {
                    String val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
                    values_list.add(val);
                }
            }
            columnNames.add(column_name); 
            result.put(column_name, values_list); 
        }

        sb.append("INSERT INTO ");
        sb.append(table.getName());
        sb.append(" (ID, VAL");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(", ");
            sb.append(columnNames.get(i));
        }
        sb.append(") VALUES ");
        for (int i = 0; i < values_length; i++) {
            sb.append(" (" + Txn_constant.getID() + ", " + Txn_constant.getVAL() + "");
            for (int j = 0; j < columnNames.size(); j++) {
                sb.append(", ");
                sb.append(result.get(columnNames.get(j)).get(i));
            }
            sb.append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(";");
        insertSql = sb.toString();
        return insertSql;
    }
}
