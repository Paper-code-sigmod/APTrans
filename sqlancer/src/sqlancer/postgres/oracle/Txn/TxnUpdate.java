package sqlancer.postgres.oracle.Txn;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

import sqlancer.Randomly;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.Txn_constant;

public class TxnUpdate {
    private String updateSql;
    private final PostgresGlobalState globalState;
    public Condition condition;
    public int nrColumns;

    public TxnUpdate(PostgresGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public String toString() {
        return updateSql;
    }

    public String genUpdateStatement(Condition condition, Map<String, List<String>> Data) {
        StringBuilder sb = new StringBuilder();
        PostgresTable table = condition.getFetchTable().getPostgresTable();
        this.condition = condition;
        List<PostgresColumn> columns = table.getColumns();
        Map<String, String> result = new HashMap<>();
        List<String> columnNames = new ArrayList<>();
        for (PostgresColumn column : columns) {
            if (Randomly.getBoolean()){
                continue;
            }
            String val = "NULL";
            String table_column_name = table.getName() + "." + column.getName();
            PostgresDataType dataType = column.getType();
            if (column.isForeignKey()) { 
                String ref_table_column_name = column.getForeignKeyReferenceStr();
                if (dataType.isNumeric()){
                    List<String> foreignData = Data.get(ref_table_column_name).stream().filter(x -> Double.parseDouble(x) < 100).collect(Collectors.toList()); 
                    val = foreignData.get(foreignData.size() - 1);
                } else {
                    val = Data.get(ref_table_column_name).get(new Random().nextInt(Data.get(ref_table_column_name).size()));
                }
            } else if (column.isUnique()) { 
                while (val.equals("NULL") || Data.get(table_column_name).contains(val)) {
                    val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
                }
            } else if (column.isNotNull()) { 
                while (val.equals("NULL")) {
                    val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
                }
            } else { 
                val = dataType.getRandomValue(globalState, column.isUnsigned).getTextRepresentation().replaceAll("\\s+", " ");
            }
            // columnNames.add(table_column_name);
            columnNames.add(column.getName());
            result.put(column.getName(), val);
        }

        sb.append("UPDATE ");
        // sb.append(condition.getJoinCondition());
        sb.append(condition.getFetchTable().getTableName());

        sb.append(" SET ");
        // sb.append(table.getName() + ".VAL = " + Txn_constant.getVAL() + ", ");
        sb.append("VAL = " + Txn_constant.getVAL());
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(", ");
            sb.append(columnNames.get(i));
            sb.append(" = ");
            sb.append(result.get(columnNames.get(i)));
        }

        // FORM条件
        String join_conditon = "";
        if(!condition.JoinTables.isEmpty()){
            sb.append(" FROM ");
            int cnt = 0;
            for (Pair<String, Pair<String, String>> joinTable : condition.JoinTables) {
                if (cnt != 0){
                    sb.append(", ");
                    join_conditon += " AND ";
                }
                sb.append(joinTable.getLeft());
                join_conditon += joinTable.getRight().getLeft() + " = " + joinTable.getRight().getRight();
                cnt++;
            }
        }

        if (join_conditon != "" || (!Randomly.getBooleanWithRatherLowProbability())){
            sb.append(" WHERE ");
            if (join_conditon != ""){
                sb.append(join_conditon);
                sb.append(" AND ");
            }
            sb.append(condition.getWhereCondition());
        }
        
        sb.append(";");
        updateSql = sb.toString();
        return updateSql;
    }
}
