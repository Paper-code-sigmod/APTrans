package sqlancer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
@Parameters(separators = "=", commandDescription = "Options for Transaction Generation")
public class MainOptions {
    @Parameter(names = "--sample_type", description = "the type of samples to generate, include: mysql, postgres or all")
    private static String sample_type = "all";

    @Parameter(names = "--sample_num", description = "total number of samples to generate")
    private static int sample_num = 100;

    @Parameter(names = "--save_path", description = "the path to save the generated cases")
    private static String save_path = "../cases/";

    @Parameter(names = "--clean_save_path", description = "the path to save the generated cases")
    private static String clean_save_path = "true";

    @Parameter(names = "--table_num", description = "the number of tables to generate in each case")
    private static int table_num = 1;

    @Parameter(names = "--random_table_num", description = "the number of tables to generate in each case")
    private static String random_table_num = "false";

    @Parameter(names = "--txn_num", description = "the number of transactions to generate in each case")
    private static int txn_num = 128;
 
    @Parameter(names = "--test_isolation", description = "the database isolation to test [serializable, repeatable_read, read_committed]")
    private static String test_isolation  = "serializable";

    public String getSample_type(){
        return sample_type;
    }

    public String getSave_path(){
        return save_path;
    }

    public boolean getClean_save_path(){
        return clean_save_path.equals("true");
    }

    public int getSample_num(){
        return sample_num;
    }

    public int getTable_num(){
        if (random_table_num.equals("true")){
            return new Randomly().getInteger(1, 4);
        } else {
            return table_num;
        }
    }

    public int getTxn_num(){
        return txn_num;
    }

    public String getTest_isolation(){
        return test_isolation;
    }

}