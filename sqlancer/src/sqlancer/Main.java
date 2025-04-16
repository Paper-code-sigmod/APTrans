package sqlancer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.oracle.MySQLTxnGen;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema;
import sqlancer.postgres.oracle.PostgresTxnGen;

public final class Main {
    static MainOptions options = new MainOptions();

    public static void MySQL_Case(String save_path) {
        if (options.getClean_save_path()) // Clean the save path
            ClearDir(save_path);
        MySQLGlobalState globalState = new MySQLGlobalState();
        Randomly r = new Randomly();
        globalState.setSchema(new MySQLSchema(new ArrayList<>()));
        globalState.setRandomly(r);

        try {
            for (int i = 0; i < options.getSample_num(); i++) {
                MySQLTxnGen txnGen = new MySQLTxnGen(globalState, options.getTable_num(), options.getTxn_num(), save_path);
                txnGen.check();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void Postgres_Case(String save_path) {
        if (options.getClean_save_path()) // Clean the save path
            ClearDir(save_path);
        PostgresGlobalState globalState = new PostgresGlobalState();
        Randomly r = new Randomly();
        globalState.setSchema(new PostgresSchema(new ArrayList<>()));
        globalState.setRandomly(r);

        try {
            for (int i = 0; i < options.getSample_num(); i++) {
                PostgresTxnGen txnGen = new PostgresTxnGen(globalState, options.getTable_num(), options.getTxn_num(), save_path);
                txnGen.check();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ClearDir(String save_path) {
        Path dirPath = Paths.get(save_path);
        if (Files.exists(dirPath)) {
            System.out.println("Cleaning the save path: " + save_path);
            try {
                Files.walk(dirPath).sorted(Comparator.reverseOrder()).map(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete " + path + ": " + e.getMessage());
                            }
                            return path;
                        }).collect(Collectors.toList()); // 收集结果（可选）
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        JCommander jCommander = new JCommander(options, args);
        jCommander.setProgramName("Transaction_Producer");

        if (options.getSample_type().equals("mysql")) {
            MySQL_Case(options.getSave_path());
        } else if (options.getSample_type().equals("postgres")) {
            Postgres_Case(options.getSave_path());
        } else if (options.getSample_type().equals("all")) {
            MySQL_Case(options.getSave_path());
            Postgres_Case( options.getSave_path());
        } else {
            System.out.println("Invalid case type");
        }
    }
}
