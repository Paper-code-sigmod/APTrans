package sqlancer.postgres.oracle.Txn;

import org.apache.commons.lang3.tuple.Pair;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLSchema;
import sqlancer.postgres.PostgresSchema.PostgresColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.RandomUtils.nextInt;

public class Condition {
    
    public enum ConditionType {
        Equal, 
        LessThan, 
        GreaterThan, 
        Or, 
        And; 
    }

    public List<TxnTable> allTables;
    //    public List<TxnTable> fromTables;
    public List<Pair<String, Pair<String, String>>> JoinTables;
    public String joinConditionString; //

    public PostgresColumn column; 
    public boolean isPredicate;
    public String val; 
    public String op; 
    public String left_val; 
    public String left_op; 
    public String right_val; 
    public String right_op; 
    public ConditionType conditionType; 
    public String whereConditionString;
    
    private TxnTable table;

    public Condition(List<TxnTable> tables, PostgresColumn column, TxnTable table, boolean isPredicate, String val) {
        this.allTables = tables;
        this.table = table;
        this.column = column;
        this.isPredicate = isPredicate;
        this.val = val;
        if (isPredicate) {
            this.conditionType = Randomly.fromOptions(ConditionType.LessThan, ConditionType.GreaterThan);
        } else {
            this.conditionType = ConditionType.Equal;
        }
        generateJoinCondition();
        generateWhereCondition();
    }

    public Condition(List<TxnTable> tables, PostgresColumn column, TxnTable table ,boolean isPredicate, String left_val, String right_val) {
        this.allTables = tables;
        this.table = table;
        this.column = column;
        this.isPredicate = isPredicate;
        this.left_val = left_val;
        this.right_val = right_val;
        this.conditionType = Randomly.fromOptions(ConditionType.Or, ConditionType.And);
        generateJoinCondition();
        generateWhereCondition();
    }

    public void generateWhereCondition(){
        switch (conditionType) {
            case Equal:
                generateEqual();
                break;
            case LessThan:
                generateLessThan();
                break;
            case GreaterThan:
                generateGreaterThan();
                break;
            case And:
                generateAnd();
                break;
            case Or:
                generateOr();
                break;
            default:
                break;
        }
        if (table.isSetUniqueKey()){
            List<PostgresColumn> uniqueColumns = table.getColumns();
            uniqueColumns = uniqueColumns.stream().filter(PostgresColumn::isUnique).collect(Collectors.toList());
            for (PostgresColumn column : uniqueColumns) {
                String table_column = table.getTableName() + "." + column.getName();
                this.whereConditionString += (" AND (" + table_column + " >= 100)");
            }
        }
    }

    public void generateEqual() {
        this.op = "=";
        this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " " + this.op + " " + val + " )";
    }

    public void generateLessThan() {
        this.op = Randomly.fromOptions("<", "<=");
        this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " " + this.op + " " + val + " )";
    }

    public void generateGreaterThan() {
        this.op = Randomly.fromOptions(">", ">=");
        this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " " + this.op + " " + val + " )";
    }

    public void generateAnd() {
        this.op = "AND";
        this.left_op = Randomly.fromOptions(">", ">=");
        this.right_op = Randomly.fromOptions("<", "<=");
        
        if (right_op.compareTo(left_op) < 0) {
            String temp = right_op;
            right_op = left_op;
            left_op = temp;
        }
        
        this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " " + this.left_op + " " + left_val + " AND "
            + table.getTableName() + "." + column.getName() + " " + this.right_op + " " + right_val + " )";

        if (this.left_op == ">=" && this.right_op == "<=" && Randomly.getBoolean()) {
            this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " BETWEEN " + left_val + " AND " + right_val + " )";
        }
    }

    public void generateOr() {
        this.op = "OR";
        this.left_op = Randomly.fromOptions(">", ">=");
        this.right_op = Randomly.fromOptions("<", "<=");
        this.whereConditionString = "( " + table.getTableName() + "." + column.getName() + " " + this.left_op + " " + left_val + " OR "
            + table.getTableName() + "." + column.getName() + " " + this.right_op + " " + right_val + ")";
    }

    public TxnTable getFetchTable() {
        return this.table;
    }

    public String getWhereCondition(){
        return this.whereConditionString;
    }

    public String getJoinCondition(){
        return this.joinConditionString;
    }

    public List<TxnTable> getAllTables() {
        return allTables;
    }

    public void generateJoinCondition() {
        List<TxnTable> AllTables = Randomly.nonEmptySubset(allTables);
        // 不使用Join方法
        if (!Randomly.getBooleanWithRatherLowProbability()) {
            AllTables.clear();
        }
        AllTables.add(0, table);
        AllTables = AllTables.stream().distinct().collect(Collectors.toList());

        List<TxnTable> FromTables = new ArrayList<>();
        this.JoinTables = new ArrayList<>(); // 必须确保引用表在前
        FromTables.add(table);

        for (TxnTable t : AllTables) {
            if (FromTables.stream().anyMatch(x -> x.getTableName().equals(t.getTableName())))  // 如果当前匹配表已经在tablesList中，跳过
                continue;

            List<Pair<String, String>> JoinTables_tmp = new ArrayList<>();
            for (PostgresColumn c : t.getColumns()) {
                if (c.isForeignKey()) {
                    String ref_table_name = c.getForeignKeyReferenceTable();
                    // 检查是否在tablesList中或者在JoinTables中，以确保引用是合法的
                    if (!ref_table_name.equals(t.getTableName())) { // 防止自引用
                        if (FromTables.stream().noneMatch(x -> x.getTableName().equals(ref_table_name)) && JoinTables.stream().noneMatch(x -> x.getLeft().equals(ref_table_name))) { // 将引用表加入tablesList
                            TxnTable ref_t = AllTables.stream().filter(x -> x.getTableName().equals(ref_table_name)).findFirst().orElse(null);
                            if (ref_t != null) { // 如果引用表存在，才进行引用，否则直接跳过
                                FromTables.add(ref_t);
                                JoinTables_tmp.add(Pair.of(t.getTableName() + "." + c.getName(), c.getForeignKeyReferenceStr()));
                            }
                        } else {
                            JoinTables_tmp.add(Pair.of(t.getTableName() + "." + c.getName(), c.getForeignKeyReferenceStr()));
                        }
                    }
                } else if (Randomly.getBoolean()) { // 查找tablesList中是否有相同数据类型的表，如果有就加入候选引用列
                    TxnTable ref_t = FromTables.stream().filter(x -> x.getColumns().stream().anyMatch(y -> y.getType().equals(c.getType()) && x != t)).findFirst().orElse(null);
                    if (ref_t != null) {
                        PostgresColumn ref_c = ref_t.getColumns().stream().filter(x -> x.getType().equals(c.getType())).findFirst().orElse(null);
                        if (ref_c != null) {
                            JoinTables_tmp.add(Pair.of(t.getTableName() + "." + c.getName(), ref_t.getTableName() + "." + ref_c.getName()));
                        }
                    }
                }
            }

            if (!JoinTables_tmp.isEmpty() && JoinTables.size() < 3) { // 如果有外键引用，就随机选一个列参与JOIN
                JoinTables.add(Pair.of(t.getTableName() ,JoinTables_tmp.get(nextInt(0, JoinTables_tmp.size()))));
            } else {
                FromTables.add(t);
            }
        }

        this.joinConditionString = " ";
        
        for (int i = 0; i < FromTables.size(); i++) {
            if (i != 0)
                this.joinConditionString = this.joinConditionString + " CROSS JOIN ";
            this.joinConditionString = this.joinConditionString + FromTables.get(i).getTableName();
        }

        if (!JoinTables.isEmpty()) {
            for (Pair<String, Pair<String, String>> joinTable : JoinTables) {
                String join_op = Randomly.fromOptions("JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN");
                this.joinConditionString = this.joinConditionString + " " + join_op + " " + joinTable.getLeft() + " ON " + joinTable.getRight().getLeft() + " = " + joinTable.getRight().getRight();
            }
        }
        this.allTables = AllTables;
    }
}
