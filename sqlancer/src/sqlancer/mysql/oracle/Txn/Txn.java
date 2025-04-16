package sqlancer.mysql.oracle.Txn;

import java.util.*;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.Txn_constant;

public class Txn {
    private final List<String> sqlStatements;
    private final MySQLGlobalState state;
    private final Map<String, List<String>> Data;

    public Txn(MySQLGlobalState globalState, Map<String, List<String>> data) {
        this.state = globalState;
        this.Data = data;
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
        String beString = "BEGIN;";
        if (Txn_constant.isolation_level != "READ COMMITTED"){
            beString = "START TRANSACTION WITH CONSISTENT SNAPSHOT;";
        }
        sqlStatements.add(0, beString);
    }

    public void parseAction(char action, Condition condition){
        String stmt;
        if (action == 'R') {
            stmt = genSelectStatement(condition);
        }
        else if (action == 'W') {
            stmt = genWriteStatement(condition);
        }
        else if (action == 'C') {
            stmt = "COMMIT;";
        }
        else{
            stmt = "ROLLBACK;";
        }
        sqlStatements.add(stmt);
    }

    public String genSelectStatement(Condition condition){
        TxnSelect select = new TxnSelect(state);
        return select.genSelectStatement(condition);
    }

    public String genWriteStatement(Condition condition){
        String statement;
        int random = new Random().nextInt(8);
        switch (random) {
            case 0:
            case 1:
                statement = genInsertStatement(condition);
                break;
            case 2:
            case 3:
            case 4:
                statement = genUpdateStatement(condition);
                break;
            case 5:
            case 6:
            case 7:
                statement = genDeleteStatement(condition);
                break;
            default:
                statement = genUpdateStatement(condition);
        }
        return statement;
    }



    public String genInsertStatement(Condition condition){
        String insertString = null;
        try {
            TxnInsert insert = new TxnInsert(state, Data);
            insertString = insert.parseInsertStatement(condition);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return insertString;
    }

    public String genUpdateStatement(Condition condition){
        String updateString = null;
        try {
            TxnUpdate update = new TxnUpdate(state, Data);
            updateString = update.parseUpdateStatement(condition);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updateString;
    }

    public String genDeleteStatement(Condition condition){
        String deleteString = null;
        try {
            TxnDelete delete = new TxnDelete();
            deleteString = delete.parseDeleteStatement(condition);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deleteString;
    }
}
