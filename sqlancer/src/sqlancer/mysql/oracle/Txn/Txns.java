package sqlancer.mysql.oracle.Txn;

import java.util.*;
import java.util.List;
import java.util.Random;

import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.Txn_constant;
import sqlancer.Randomly;

public class Txns {
    private final List<Txn> transactions;
    private final MySQLGlobalState state;
    public Txn_constant.Pattern Txn_Pattern;
    public String Schedule;
    public int Txn_var_num;
    public TxnTable table;
    public List<Condition> fetch_exps;

    public Txns(MySQLGlobalState globalState, TxnTable txnTable) { // 带Table
        this.state = globalState;
        this.table = txnTable;
        this.transactions = new ArrayList<>();
        this.fetch_exps = new ArrayList<>();
    }

    public Txns(MySQLGlobalState globalState) {
        this.state = globalState;
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

    public void genTxns(Map<String, List<String>> data) {
        switch (Txn_var_num) {
            case 1:
                genTxnPattern1(data);
                break;
            case 2:
                genTxnPattern2(data);
                break;
            case 3:
                genTxnPattern3(data);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + Txn_var_num);
        }
    }


    public void genRandomTxn(Map<String, List<String>> data) {
        // 创建两个事务对象
        Txn txn1 = new Txn(state, data); // 事务1
        Txn txn2 = new Txn(state, data); // 事务2

        // 随机生成事务1的SQL数目
        int sql_num_1 = new Random().nextInt(3); // 生成1到10之间的随机数
        // 随机生成事务2的SQL数目
        int sql_num_2 = new Random().nextInt(3); // 生成1到10之间的随机数

        // 为事务1生成随机SQL语句
        for (int i = 0; i < sql_num_1; i++) {
            // 随机选择一条SQL语句
            char action = Randomly.fromList(List.of('R', 'W'));
            Condition condition = fetch_exps.get(new Random().nextInt(10));
            txn1.parseAction(action, condition);
        }
        // 为事务2生成随机SQL语句
        for (int i = 0; i < sql_num_2; i++) {
            // 随机选择一条SQL语句
            char action = Randomly.fromList(List.of('R', 'W'));
            Condition condition = fetch_exps.get(new Random().nextInt(10));
            txn2.parseAction(action, condition);
        }

        txn1.packageTxn();
        txn2.packageTxn();

        txn1.addStatement("COMMIT;");
        txn2.addStatement("COMMIT;");

        addTransaction(txn1);
        addTransaction(txn2);
    }

    public void genTxnPattern1(Map<String, List<String>> data){
        Txn txn1 = new Txn(state, data); // 事务1
        Txn txn2 = new Txn(state, data); // 事务2

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);

            Condition condition = fetch_exps.get(0);

            if (txn_idx == '1') {
                txn1.parseAction(action, condition);
            }
            else{
                txn2.parseAction(action, condition);
            }
        }

        txn1.packageTxn();
        txn2.packageTxn();

        addTransaction(txn1);
        addTransaction(txn2);
    }

    public void genTxnPattern2(Map<String, List<String>> data){
        Txn txn1 = new Txn(state, data);
        Txn txn2 = new Txn(state, data);

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);
            char variable = Txn_Pattern.charAt(i + 2);
            Condition condition;
            
            if (variable == 'x' || variable == 'X'){
                condition = fetch_exps.get(0);
            }
            else {
                condition = fetch_exps.get(1);
            }

            if (txn_idx == '1') {
                txn1.parseAction(action, condition);
            }
            else{
                txn2.parseAction(action, condition);
            }
        }

        txn1.packageTxn();
        txn2.packageTxn();

        addTransaction(txn1);
        addTransaction(txn2);
    }

    public void genTxnPattern3(Map<String, List<String>> data){
        Txn txn1 = new Txn(state, data);
        Txn txn2 = new Txn(state, data);
        Txn txn3 = new Txn(state, data);

        for (int i = 0; i < Txn_Pattern.length(); i += 3) {
            char action = Txn_Pattern.charAt(i);
            char txn_idx = Txn_Pattern.charAt(i + 1);
            char variable = Txn_Pattern.charAt(i + 2);
            Condition condition;
            
            if (variable == 'x'){
                condition = fetch_exps.get(0);
            }
            else if (variable == 'y') {
                condition = fetch_exps.get(1);
            }
            else {
                condition = fetch_exps.get(2);
            }

            if (txn_idx == '1') {
                txn1.parseAction(action, condition);
            }
            else if (txn_idx == '2') {
                txn2.parseAction(action, condition);
            }
            else{
                txn3.parseAction(action, condition);
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
        // begin、select
        for (int i = 1; i <= txn_num; i++) {
            txn_list.add(i);
        }
        for (int i = 0; i < pattern.length(); i += 3) {
            int txn_idx = pattern.charAt(i + 1) - '0';
            txn_list.add(txn_idx);
        }
        // 转换为字符串并使用连接符连接
        List<String> strList = new ArrayList<>();
        for (Integer txn : txn_list) {
            strList.add(txn.toString());
        }
        Schedule = String.join(",", strList);
    }

    public void genRandomSchedule() {
        ArrayList<Integer> txn_list = new ArrayList<>();
        int txn_num = transactions.size();
        for (int i = 0; i < txn_num; i++) {
            for (int j = 0; j < transactions.get(i).getSqlStatements().size() - 1; j++) {
                txn_list.add(i + 1); // 假设事务i+1的SQL语句被添加
            }
        }

        // shuffle：打乱列表
        Collections.shuffle(txn_list);
        // 生成事务SQL语句的调度顺序
        for (int i = txn_num; i > 0; i--) {
            txn_list.add(0, i);
        }

        // 转换为字符串并使用连接符连接
        List<String> strList = new ArrayList<>();
        for (Integer txn : txn_list) {
            strList.add(txn.toString());
        }
        Schedule = String.join(",", strList);
    }

    
    public void genPattern(){
        // DO: Get random Pattern
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
