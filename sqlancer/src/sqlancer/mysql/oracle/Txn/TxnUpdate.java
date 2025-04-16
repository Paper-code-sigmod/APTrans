package sqlancer.mysql.oracle.Txn;

import sqlancer.Randomly;
import sqlancer.Txn_constant;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.ast.MySQLConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TxnUpdate {
    private final MySQLGlobalState state;
    private final Map<String, List<String>> Data;

    public TxnUpdate(MySQLGlobalState state, Map<String, List<String>> Data) {
        this.state = state;
        this.Data = Data;
    }

    public String parseUpdateStatement(Condition condition) {
        MySQLSchema.MySQLTable table = condition.getFetchTable().getMySQLTable();


        Map<String, String> update_data = new HashMap<>();
        List<String> update_columns = new ArrayList<>();
        for (MySQLSchema.MySQLColumn column: table.getColumns()){
            if (Randomly.getBoolean()){
                continue;
            }
            String table_column_name = (table.getName() + "." + column.getName()).trim();
            update_columns.add(table_column_name);
            MySQLSchema.MySQLDataType dataType = column.getType();
            if (column.isForeignKey()) {
                String foreignTable_column_name = column.getForeignKeyReferenceStr();
                if (Data.get(foreignTable_column_name).isEmpty()){
                    update_data.put(table_column_name, "NULL");
                } else {
                    List<String> foreignData = Data.get(foreignTable_column_name).stream().filter(x -> Double.parseDouble(x) < 100).collect(Collectors.toList());
                    String val = foreignData.get(foreignData.size() - 1);
                    update_data.put(table_column_name, val);
                }
            } else {
                if (column.isNotNull()) {
                    String val = "NULL";
                    while (val.equals("NULL")) {
                        MySQLConstant value = dataType.getRandomValue(state, column.isUnsigned);
                        val = value.getTextRepresentation().replaceAll("\\s+", " ");
                    }
                    update_data.put(table_column_name, val);
                } else {
                    MySQLConstant value = dataType.getRandomValue(state, column.isUnsigned);
                    String val = value.getTextRepresentation().replaceAll("\\s+", " ");
                    update_data.put(table_column_name, val);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ");
        sb.append(condition.getJoinCondition());
        sb.append(" SET ");
        sb.append("VAL = " + Txn_constant.getVAL());
        for (int i = 0; i < update_columns.size(); i++) {
            sb.append(", ");
            sb.append(update_columns.get(i)).append(" = ").append(update_data.get(update_columns.get(i)));
        }
        if (!Randomly.getBooleanWithRatherLowProbability()){
            sb.append(" WHERE ");
            sb.append(condition.getWhereCondition());
        }
        sb.append(";");
        return sb.toString();
    }
}
