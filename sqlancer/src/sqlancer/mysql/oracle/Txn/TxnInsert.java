package sqlancer.mysql.oracle.Txn;

import sqlancer.Randomly;
import sqlancer.Txn_constant;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.ast.MySQLConstant;

import java.util.*;
import java.util.stream.Collectors;

public class TxnInsert {
    private final MySQLGlobalState state;
    private final Map<String, List<String>> Data;

    public TxnInsert(MySQLGlobalState globalState, Map<String, List<String>> data) {
        this.state = globalState;
        this.Data = data;
    }

    public String parseInsertStatement(Condition condition) {
        MySQLSchema.MySQLTable table = condition.getFetchTable().getMySQLTable();

        Map<String, List<String>> result = new HashMap<>();
        List<String> columnNames = new ArrayList<>();
        int values_length = new Random().nextInt(2) + 1;
        List<MySQLSchema.MySQLColumn> columns = table.getColumns();
        for (MySQLSchema.MySQLColumn column : columns) {
            List<String> values_list = new ArrayList<>();
            String table_column_name = table.getName() + "." + column.getName();
            if (Randomly.getBooleanWithSmallProbability() && !column.isPrimaryKey() && !column.isNotNull() && !column.isUnique() && !columnNames.isEmpty()) {
                for (int i = 0; i < values_length; i++) {
                    values_list.add("NULL");
                }
//                Data.get(table_column_name).addAll(values_list);
                continue;
            }
            String now_column_name = column.getName();
            MySQLSchema.MySQLDataType dataType = column.getType();
            if (column.isForeignKey()) {
                String ref_table_column_name = column.getForeignKeyReferenceStr();
                for (int i = 0; i < values_length; i++) {
                    if (Data.get(ref_table_column_name).isEmpty()){
//                        Data.get(table_column_name).add("NULL");
                        values_list.add("NULL");
                    } else {
                        List<String> foreignData = Data.get(ref_table_column_name).stream().filter(x -> Double.parseDouble(x) < 100).collect(Collectors.toList());
                        String ref_val = foreignData.get(foreignData.size() - 1);
//                        Data.get(table_column_name).add(ref_val);
                        values_list.add(ref_val);
                    }
                }
            } else if (column.isUnique() || column.isPrimaryKey()) {
                for (int i = 0; i < values_length; i++) {
                    while (true) {
                        MySQLConstant value = dataType.getRandomValue(state, column.isUnsigned);
                        String val = value.getTextRepresentation().replaceAll("\\s+", " ");
                        while (val.equals("NULL")) {
                            value = dataType.getRandomValue(state, column.isUnsigned);
                            val = value.getTextRepresentation().replaceAll("\\s+", " ");
                        }
                        if (Data.get(table_column_name).isEmpty() || !Data.get(table_column_name).contains(val)) {
//                            Data.get(table_column_name).add(val);
                            values_list.add(val);
                            break;
                        }
                    }
                }
            } else if (column.isNotNull()) {
                for (int i = 0; i < values_length; i++) {
                    MySQLConstant value = dataType.getRandomValue(state, column.isUnsigned);
                    String val = value.getTextRepresentation().replaceAll("\\s+", " ");
                    while (val.equals("NULL")) {
                        value = dataType.getRandomValue(state, column.isUnsigned);
                        val = value.getTextRepresentation().replaceAll("\\s+", " ");
                    }
//                    Data.get(table_column_name).add(val);
                    values_list.add(val);
                }
            } else {
                for (int i = 0; i < values_length; i++) {
                    MySQLConstant value = dataType.getRandomValue(state, column.isUnsigned);
                    String val = value.getTextRepresentation().replaceAll("\\s+", " ");
//                    Data.get(table_column_name).add(val);
                    values_list.add(val);
                }
            }
            columnNames.add(now_column_name);
            result.put(now_column_name, values_list);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table.getName()).append(" (ID, VAL, ");
        for (int i = 0; i < columnNames.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columnNames.get(i));
        }
        sb.append(") VALUES ");
        for (int i = 0; i < values_length; i++) {
            sb.append("( ");
            sb.append(Txn_constant.getID() + ", " + Txn_constant.getVAL());
            for (int j = 0; j < columnNames.size(); j++) {
                sb.append(", ");
                sb.append(result.get(columnNames.get(j)).get(i));
            }
            sb.append(")");
            if (i != values_length - 1) {
                sb.append(", ");
            }
        }
        sb.append(";");
        return sb.toString();
    }
}
