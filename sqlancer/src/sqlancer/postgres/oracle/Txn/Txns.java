package sqlancer.postgres.oracle.Txn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sqlancer.postgres.PostgresGlobalState;
import sqlancer.Txn_constant;

public class Txns {
    private final List<Txn> transactions;
    private final PostgresGlobalState state;
    public Txn_constant.Pattern Txn_Pattern;
    public String Schedule;
    public int Txn_var_num;
    public List<Condition> fetch_exps;
    public Map<String, List<String>> Data;

    public Txns(PostgresGlobalState globalState, Map<String, List<String>> data) {
        this.state = globalState;
        this.Data = data;
        this.transactions = new ArrayList<>();
        this.fetch_exps = new ArrayList<>();
    }

    public void addTransaction(Txn txn) {
        transactions.add(txn);
    }

    public void setFetchSet(List<Condition> fetch_exps) {
        this.fetch_exps = fetch_exps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transactions:\n");
        for (Txn txn : transactions) {
            sb.append(txn.toString()).append("\n");
        }
        return sb.toString();
    }

    public List<String> getString() {
        List<String> ret = new ArrayList<>();
        for (Txn txn : transactions) {
            ret.add(txn.getString().toString());
        }
        return ret;
    }

    public void genTxns() {
        switch (Txn_var_num) {
            case 1:
                genTxnPattern1();
                break;
            case 2:
                genTxnPattern2();
                break;
            case 3:
                genTxnPattern3();
                break;
            default:
                genTxnPattern1();
        }
    } 

    public String genWriteStatement(Condition condition){
        String statement;
        int random = new Random().nextInt(8);
        switch (random) {
            case 0:
            case 1:
                TxnInsert insert = new TxnInsert(state);
                statement = insert.genInsertStatement(condition, Data);
                break;
            case 2:
            case 3:
            case 4:
                TxnUpdate update = new TxnUpdate(state);
                statement = update.genUpdateStatement(condition, Data);
                break;
            case 5:
            case 6:
            case 7:
                TxnDelete delete = new TxnDelete();
                statement = delete.genDeleteStatement(condition);
                break;
            default:
                statement = "";
        }
        return statement;
    }

    public void genTxnPattern1(){

        Txn txn1 = new Txn(state);
        Txn txn2 = new Txn(state);

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);

            String stmt;
            Condition condition = fetch_exps.get(0);
            
            if (action == 'R') {
                TxnSelect select = new TxnSelect(state);
                stmt = select.genSelectStatement(condition);
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

            if (txn_idx == '1') {
                txn1.addStatement(stmt);
            }
            else{
                txn2.addStatement(stmt);
            }
        }

        txn1.packageTxn();
        txn2.packageTxn();

        addTransaction(txn1);
        addTransaction(txn2);
    }

    public void genTxnPattern2(){
        Txn txn1 = new Txn(state);
        Txn txn2 = new Txn(state);

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);
            char variable = Txn_Pattern.charAt(i + 2);
            Condition restriction;
            String stmt;

            if (variable == 'x' || variable == 'X'){
                restriction = fetch_exps.get(0);
            }
            else {
                restriction = fetch_exps.get(1);
            }
            if (action == 'R') {
                TxnSelect select = new TxnSelect(state);
                stmt = select.genSelectStatement(restriction);
            }
            else if (action == 'W') {
                stmt = genWriteStatement(restriction);
            }
            else if (action == 'C') {
                stmt = "COMMIT;";
            }
            else{
                stmt = "ROLLBACK;";
            }

            if (txn_idx == '1') {
                txn1.addStatement(stmt);
            }
            else{
                txn2.addStatement(stmt);
            }
        }

        txn1.packageTxn();
        txn2.packageTxn();

        addTransaction(txn1);
        addTransaction(txn2);
    }

    public void genTxnPattern3(){
        Txn txn1 = new Txn(state);
        Txn txn2 = new Txn(state);
        Txn txn3 = new Txn(state);

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);
            char variable = Txn_Pattern.charAt(i + 2);
            Condition restriction;
            String stmt;
            
            if (variable == 'x'){
                restriction = fetch_exps.get(0);
            }
            else if (variable == 'y') {
                restriction = fetch_exps.get(1);
            }
            else {
                restriction = fetch_exps.get(2);
            }
            
            if (action == 'R') {
                TxnSelect select = new TxnSelect(state);
                stmt = select.genSelectStatement(restriction);
            }
            else if (action == 'W') {
                stmt = genWriteStatement(restriction);
            }
            else if (action == 'C') {
                stmt = "COMMIT;";
            }
            else{
                stmt = "ROLLBACK;";
            }

            if (txn_idx == '1') {
                txn1.addStatement(stmt);
            }
            else if (txn_idx == '2') {
                txn2.addStatement(stmt);
            }
            else{
                txn3.addStatement(stmt);
            }
        }

        txn1.packageTxn();
        txn2.packageTxn();
        txn3.packageTxn();

        addTransaction(txn1);
        addTransaction(txn2);
        addTransaction(txn3);
    }

    public void genSchedule() {
        ArrayList<Integer> txn_list = new ArrayList<>();
        int txn_num = transactions.size();
        Txn_constant.Pattern pattern = Txn_Pattern;
        
        for (int i = 1; i <= txn_num; i++) {
            txn_list.add(i);
        }
        for (int i = 0; i < pattern.length(); i += 3) {
            int txn_idx = pattern.charAt(i + 1) - '0';
            txn_list.add(txn_idx);
        }
        
        List<String> strList = new ArrayList<>();
        for (Integer txn : txn_list) {
            strList.add(txn.toString());
        }
        Schedule = String.join(",", strList);
    }

    public void genPattern(){
        
        int random = new Random().nextInt(6);
        switch (random) {
            case 0:
            case 1:
            case 2:
            case 3:
                Txn_var_num = 1;
                this.Txn_Pattern = Txn_constant.getPattern(1);
                break;
            case 4:
            case 5:
                Txn_var_num = 2;
                this.Txn_Pattern = Txn_constant.getPattern(2);
                break;
        }

    }
}
