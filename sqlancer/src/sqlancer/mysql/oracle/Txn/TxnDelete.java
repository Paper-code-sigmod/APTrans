package sqlancer.mysql.oracle.Txn;

import sqlancer.Randomly;

public class TxnDelete {

    public String parseDeleteStatement(Condition condition){
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE ");
        sb.append(condition.getFetchTable().getTableName());
        sb.append(" FROM ");
        sb.append(condition.getJoinCondition());
        if (!Randomly.getBooleanWithRatherLowProbability()){
            sb.append(" WHERE ");
            sb.append(condition.getWhereCondition());
        }
        sb.append(";");
        return sb.toString();
    }
}
