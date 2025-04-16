package sqlancer.postgres.oracle.Txn;

import java.util.List;
import java.util.ArrayList;

import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.Txn_constant;

public class Txn {
    private final List<String> sqlStatements;
    private final PostgresGlobalState state;

    public Txn(PostgresGlobalState globalState) {
        this.state = globalState;
        this.sqlStatements = new ArrayList<>();
    }

    public void addStatement(String sql) {
        sqlStatements.add(sql);
    }

    public List<String> getSqlStatements() {
        return sqlStatements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction:\n");
        for (String sql : sqlStatements) {
            sb.append(sql).append("\n");
        }
        return sb.toString();
    }

    public List<String> getString() {
        List<String> ret = new ArrayList<>();
        for (String sql : sqlStatements) {

            String formattedSql = sql.replaceAll("\\s+", " ").trim();
            ret.add(formattedSql);
        }
        return ret;
    }

    public void packageTxn(){
        String beString = "START TRANSACTION;";
        if (Txn_constant.isolation_level != "READ COMMITTED"){
            beString += "SELECT 1;"; // 创建快照
        }
        sqlStatements.add(0, beString);
        boolean ser = true;
        if(!ser){
            PostgresTable MySQLtable = state.getSchema().getTable();
            String table_name = MySQLtable.getName();
            String select_sql = "SELECT * FROM " + table_name + ";";
            sqlStatements.add(1, select_sql);
            sqlStatements.add(select_sql);
        }
    }
}
