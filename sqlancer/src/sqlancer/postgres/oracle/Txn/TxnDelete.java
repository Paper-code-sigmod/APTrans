package sqlancer.postgres.oracle.Txn;
import org.apache.commons.lang3.tuple.Pair;

import sqlancer.Randomly;

public class TxnDelete {
    private String deleteSql;

    public TxnDelete() {
    }

    @Override
    public String toString() {
        return deleteSql;
    }

    public String genDeleteStatement(Condition condition){
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE ");
        // sb.append(condition.getFetchTable().getTableName());
        sb.append("FROM ");
        // sb.append(condition.getJoinCondition());
        // FORM条件
        sb.append(condition.getFetchTable().getTableName());
        
        String join_conditon = "";
        if(!condition.JoinTables.isEmpty()){
            sb.append(" USING ");
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
        deleteSql = sb.toString();
        return deleteSql;
    }
}
