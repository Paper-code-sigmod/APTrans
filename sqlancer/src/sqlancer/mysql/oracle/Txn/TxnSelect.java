package sqlancer.mysql.oracle.Txn;

import org.apache.commons.lang3.tuple.Pair;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLSchema.MySQLTables;
import sqlancer.mysql.MySQLSchema.MySQLTable;

import static org.apache.commons.lang3.RandomUtils.nextInt;

public class TxnSelect {
    private String selectSql;
    private final MySQLGlobalState globalState;

    public TxnSelect(TxnTable table, MySQLGlobalState globalState) {
        this.globalState = globalState;
        this.selectSql = "";
    }

    public TxnSelect(MySQLGlobalState globalState) {
        this.globalState = globalState;
        this.selectSql = "";
    }

    @Override
    public String toString() {
        return "Select SQL: " + selectSql;
    }

    public String genSelectStatement(Condition condition) {
        StringBuilder sb = new StringBuilder();
        List<String> columns = new ArrayList<>();

        for (TxnTable t : condition.getAllTables()) {
            if (t.getTableName().equals(condition.getFetchTable().getTableName())){
                for (MySQLSchema.MySQLColumn c : t.getColumns()){
                    columns.add(t.getTableName() + "." + c.getName());
                }
            } else {
                for (int i = 0; i < t.getColumns().size(); i++) {
                    if (Randomly.getBoolean() || i == 0) {
                        columns.add(t.getTableName() + "." + t.getColumns().get(i).getName());
                    }
                }
            }
        }

        sb.append("SELECT ");

        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i));
        }

        sb.append(", INFO FROM ");
        sb.append(condition.getJoinCondition());
        if (!Randomly.getBooleanWithRatherLowProbability()){
            sb.append(" WHERE ");
            sb.append(condition.getWhereCondition());
        }
        // order by
        if (Randomly.getBoolean()){
            sb.append(" ORDER BY ");
            for(int i = 0; i < condition.getAllTables().size(); i++){
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(condition.getAllTables().get(i).getTableName() + ".ID");
            }
        }
        
        if (Randomly.getBooleanWithRatherLowProbability()){
            sb.append(" FOR UPDATE");
            // if (Randomly.getBoolean()){
            //     sb.append(" SKIP LOCKED");
            // }
        }
        sb.append(";");
        selectSql = sb.toString();
        return selectSql;
    }
}
