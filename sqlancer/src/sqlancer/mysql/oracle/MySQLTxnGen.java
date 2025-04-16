package sqlancer.mysql.oracle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.Txn_constant;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.oracle.Txn.TxnTable;
import sqlancer.mysql.oracle.Txn.Cases;

public class MySQLTxnGen {
    private final MySQLGlobalState state;
    public List<TxnTable> tables = new ArrayList<>();
    public static int foreign_key_count = 0;
    public int table_num = 3;
    public int txn_num = 1000;
    public String save_path = "./cases";

    public MySQLTxnGen(MySQLGlobalState globalState, int table_num, int txn_num, String save_path) {
        this.table_num = table_num;
        this.save_path = save_path;
        this.txn_num = txn_num;
        this.state = globalState;
    }

    public void check() throws Exception {
        Path dirPath = Paths.get(save_path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String fileName = "test_sample_" + System.currentTimeMillis() + Randomly.getNotCachedInteger(0, 10000) + ".txt";
        Path filePath = dirPath.resolve(fileName);

        state.getSchema().clearTable();
        tables.clear();

        Txn_constant.setIsolationLevel();

        for (int i = 0; i < table_num; i++) {
            TxnTable table = new TxnTable(state);
            MySQLTable sqlTable = table.getMySQLTable();
            tables.add(table);
            state.getSchema().addTable(sqlTable);
        }

        Cases test_case = new Cases(state, tables, txn_num);
        test_case.generateCase();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toString()), StandardCharsets.UTF_8))) {
            writer.write("Tables Num: " + tables.size() + "\n");
            for (int i = 0; i < tables.size(); i++) {
                writer.write("Table " + i + ": " + tables.get(i).getTableName() + "\n");
                writer.write("Create SQL " + i + ": " + tables.get(i).getCreateSql() + "\n");
            }
            writer.write("Case Num: " + test_case.all_cases.size() + "\n");
            writer.write(test_case.toString());
            System.out.println("Text file created successfully: " + filePath);
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
        }
    }

}