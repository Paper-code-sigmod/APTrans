package sqlancer.postgres.oracle.Txn;

import java.util.*;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.ast.PostgresConstant;
import sqlancer.Txn_constant;

public class Cases {
    public int case_num;
    public int table_num;
    private final PostgresGlobalState state;
    public TxnTable table;
    public List<List<String>> Data_Sql;

    public List<TxnTable> tables;
    public static Map<String, List<String>> Data; 
    public List<Txns> all_cases; 

    public Cases(PostgresGlobalState globalState) {
        this.state = globalState;
        this.all_cases = new ArrayList<>();
    }

    public Cases(PostgresGlobalState globalState, List<TxnTable> txnTables, int case_num) {
        this.state = globalState;
        this.tables = txnTables; 
        this.table_num = txnTables.size();
        this.case_num = case_num;
        this.all_cases = new ArrayList<>();
        this.Data_Sql = new ArrayList<>();
        Data = new HashMap<>();
        Data_Map_Init();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < all_cases.size(); i++){
            sb.append("Data SQL ").append(i).append(": [");
            for (int j = i * table_num; j < (i + 1) * table_num; j++){
                for (String sql: Data_Sql.get(j)){ 
                    sb.append(sql).append(", ");
                }
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]\n");
            sb.append("VAR Num ").append(i).append(": ");
            sb.append(all_cases.get(i).Txn_var_num + "\n");
            sb.append("Pattern ").append(i).append(": ");
            sb.append(all_cases.get(i).Txn_Pattern.getPatternDescription() + "\n");
            sb.append("Schedule ").append(i).append(": ");
            sb.append(all_cases.get(i).Schedule + "\n");
            sb.append("Case ").append(i).append(": ");
            sb.append(all_cases.get(i).getString() + "\n");
        }
        return sb.toString();
    }

    public void generateCase(){
        for (int idx = 0; idx < case_num; idx++) {
            Data_Load_MultiTables();
            Txns txns = new Txns(state, Data);
            txns.genPattern();
            List<Condition> fetch_set = new ArrayList<>();
            for (int i = 0; i < txns.Txn_var_num; i++){
                Condition expString = gen_fetch_exp(Txn_constant.isPredicate(txns.Txn_Pattern));
                fetch_set.add(expString);
            }
            txns.setFetchSet(fetch_set);
            txns.genTxns();
            txns.genSchedule();
            all_cases.add(txns);

        }
    }

    public void Data_Map_Init(){
        for (TxnTable table: tables) {
            List<PostgresColumn> columns = table.getColumns();
            String table_name = table.getTableName();
            for (int i = 0; i < columns.size(); i++) {
                PostgresColumn column = columns.get(i);
                String column_name = column.getName();
                Data.put(table_name + "." + column_name, new ArrayList<>());
            }
        }
    }

    public void Data_Load_MultiTables(){
//        Data.clear();
//        Data_Map_Init();
        int Insert_Cnt = new Random().nextInt(3) + 2;
        try{
            for (TxnTable table: tables) {
                List<String> Load_Sqls = new ArrayList<>();
                List<PostgresColumn> columns = table.getColumns();
                for (int i = 0; i < Insert_Cnt; i++) {
                    StringBuilder InitBuilder = new StringBuilder();
                    InitBuilder.append("INSERT INTO ");
                    InitBuilder.append(table.getTableName());
                    InitBuilder.append(" (ID, VAL");
                    for (int j = 0; j < columns.size(); j++)
                        InitBuilder.append(", " + columns.get(j).getName());
                    InitBuilder.append(")");
                    InitBuilder.append(" VALUES (");
                    InitBuilder.append(Txn_constant.getID());
                    InitBuilder.append(", ");
                    InitBuilder.append(Txn_constant.getVAL());
                    InitBuilder.append(", ");
                    for (int j = 0; j < columns.size(); j++) {
                        if (j > 0) {
                            InitBuilder.append(", ");
                        }
                        PostgresColumn dataColumn = columns.get(j);
                        PostgresDataType dataType = dataColumn.getType();
                        String table_column_name = table.getTableName() + "." + dataColumn.getName();
                        if (dataColumn.isForeignKey()) {
                            String foreignTable_column_name = dataColumn.getForeignKeyReferenceStr();
                            String val;
                            if (dataType.isNumeric()) {
                                List<Double> foreignData = new ArrayList<>();
                                for (String v: Data.get(foreignTable_column_name)){
                                    foreignData.add(Double.parseDouble(v));
                                }
                                foreignData = foreignData.stream().filter(x -> x < 100).collect(Collectors.toList());
//                                val = foreignData.get(new Random().nextInt(foreignData.size())).toString();
                                val = foreignData.get(foreignData.size() - 1).toString();
//                                val = foreignData.get(new Random().nextInt(Insert_Cnt)).toString();

                            } else {
                                val = Data.get(foreignTable_column_name).get(new Random().nextInt(Data.get(foreignTable_column_name).size()));
                            }
                            Data.get(table_column_name).add(val);
                            if (val == "NULL")
                                System.out.println("NULL1: " + table_column_name);

                            InitBuilder.append(val);
                        } else {
                            if (dataColumn.isUnique()) {
                                String val = "NULL";

                                while (val.equals("NULL") || (Data.get(table_column_name).contains(val) || (Data.get(table_column_name).isEmpty() && Double.parseDouble(val) >= 100))) {
                                    PostgresConstant value = dataType.getRandomValue(state, dataColumn.isUnsigned);
                                    val = value.getTextRepresentation().replaceAll("\\s+", " ");
                                }
                                Data.get(table_column_name).add(val);
                                InitBuilder.append(val);
                            } else if (dataColumn.isNotNull()) {
                                String val = "NULL";
                                while (val.equals("NULL")) {
                                    PostgresConstant value = dataType.getRandomValue(state, dataColumn.isUnsigned);
                                    val = value.getTextRepresentation().replaceAll("\\s+", " ");
                                }
                                Data.get(table_column_name).add(val);
                                InitBuilder.append(val);
                            } else {
                                PostgresConstant value = dataType.getRandomValue(state, dataColumn.isUnsigned);
                                String val = value.getTextRepresentation().replaceAll("\\s+", " ");
                                Data.get(table_column_name).add(val);
                                InitBuilder.append(val);
                            }
                        }
                    }
                    InitBuilder.append(");");
                    Load_Sqls.add(InitBuilder.toString());
                }
                Data_Sql.add(Load_Sqls);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public int RandomRow(int length) {
        Randomly randomly = new Randomly();
        long n = length * length;
        long randval = randomly.getLong(0, n);
        int randomIndex = (int) Math.sqrt(randval);
        return Math.min(randomIndex, length - 1);
    }

    public Condition gen_fetch_exp(boolean isPredicate) {
        int max_tries;
        int total_tries = 10;
        boolean right_exp;
        int table_num = tables.size();
        TxnTable table; 
        Condition condition;
        try{
            do { 
                table = tables.get(new Random().nextInt(table_num));
                PostgresColumn column = table.getColumns().get(new Random().nextInt(table.getColumns().size()));
                String table_column_name = table.getTableName() + "." + column.getName();
                right_exp = true;
                if (Randomly.getBoolean()) {

                    int row_idx = RandomRow(Data.get(table_column_name).size());
                    String val = Data.get(table_column_name).get(row_idx);
                    max_tries = 10;
                    while (val.equals("NULL") && max_tries > 0){
                        max_tries--;
                        row_idx = RandomRow(Data.get(table_column_name).size());
                        val = Data.get(table_column_name).get(row_idx);
                    }
                    if (max_tries == 0) right_exp = false;
                    condition = new Condition(tables, column, table, isPredicate, val);
                } else {
                    int row_idx1 = RandomRow(Data.get(table_column_name).size());
                    String val1 = Data.get(table_column_name).get(row_idx1);
                    max_tries = 10;
                    while (val1.equals("NULL") && max_tries > 0){
                        max_tries--;
                        row_idx1 = RandomRow(Data.get(table_column_name).size());
                        val1 = Data.get(table_column_name).get(row_idx1);
                    }
                    if (max_tries == 0) right_exp = false;
                    int row_idx2 = RandomRow(Data.get(table_column_name).size());
                    String val2 = Data.get(table_column_name).get(row_idx2);
                    max_tries = 10;
                    while ((row_idx1 == row_idx2 || val2.equals("NULL")) && max_tries > 0){
                        max_tries--;
                        row_idx2 = RandomRow(Data.get(table_column_name).size());
                        val2 = Data.get(table_column_name).get(row_idx2);
                    }
                    if (max_tries == 0) right_exp = false;
                    condition = new Condition(tables, column, table, isPredicate, val1, val2);
                }
                total_tries--;
            } while (!right_exp && total_tries > 1);
        } catch (Exception e){
            e.printStackTrace();
            throw new AssertionError("Cannot generate a valid fetch expression");
        }
        return condition;
    }
}