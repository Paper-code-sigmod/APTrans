# Usage
## 1. run in the terminal
```bash
  mvn clean package
  java -jar ./target/sqlancer-2.0.0.jar --sample_type all --sample_num 10 --txn_num 10 --save_path ./cases/
```
## 2. parameters
- --save_path: [your_path], the path to save the generated files, default is ../cases/.
- --sample_type: [all, mysql, postgres], default is all.
- --sample_num: [100], the number of transaction testing files to generate, default is 100.
- --clean_save_path: [true or false], whether clean the save path before generating new files, default is false.
- --table_num: [3], the number of tables you want to generate in each sample, default is 3.
- --random_table_num: [true or false], whether the number of tables of each sample is random, from 1 to 5, default is false.
- --txn_num: [1000], the number of transactions you want to generate in each sample, default is 1000.

## 3. key word dictionary

["SELECT", "FROM", "WHERE", "AND", "OR", "JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "ORDER BY", "FOR UPDATE", "UPDATE", "SET", "INSERT INTO", "VALUES", "DELETE", "=", ">", "<", ">=", "<=", "IN", "BETWEEN", "BEGIN", "COMMIT", "ROLLBACK"]