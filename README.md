# APTrans

APTrans (Anomaly Pattern-guided Transaction) is a tool designed to detect transaction bugs in Relational Database Management Systems (RDBMSs) with high accuracy and efficiency. The tool generates test cases and captures anomalies based on predefined exception patterns. In our study, we applied APTrans to test three widely used databases—MySQL, MariaDB, and OceanBase—leading to the identification of 13 unique transaction bugs.

For more detailed information, please refer to the paper titled **"Anomaly Pattern-guided Transaction Bug Testing in Relational Databases"**.

# Overview

- **Bugs**: A list of the identified bugs.
- **Comparison**: Comparison of results between different methods.
- **SQLancer**: Modifications made to SQLancer for transaction generation.
- **Executor**: Description of the executor and checker components.

# Requirements

- Java 11 or higher
- Maven
- Python

# Usage

- `./generate.sh`: Generate transaction test cases.
- `./test.sh`: Test all databases.

# Configuration

To configure database parameters, update the relevant scripts located in `/executor/db_config.py` and `/sqlancer/src/sqlancer/MainOptions.java`.

# Bugs

Below is the list of transaction bugs identified during testing:

| ID  | Database   | Bug ID        | Link                                                              |
|-----|------------|---------------|-------------------------------------------------------------------|
| 1   | MySQL      | Bug#115978    | [Bug#115978](https://bugs.mysql.com/bug.php?id=115978)            |
| 2   | MySQL      | Bug#117218    | [Bug#117218](https://bugs.mysql.com/bug.php?id=117218)            |
| 3   | MySQL      | Bug#117733    | [Bug#117733](https://bugs.mysql.com/bug.php?id=117733)            |
| 4   | MySQL      | Bug#117797    | [Bug#117797](https://bugs.mysql.com/bug.php?id=117797)            |
| 5   | MySQL      | Bug#117835    | [Bug#117835](https://bugs.mysql.com/bug.php?id=117835)            |
| 6   | MySQL      | Bug#117860    | [Bug#117860](https://bugs.mysql.com/bug.php?id=117860)            |
| 7   | MariaDB    | MDEV-35335    | [MDEV-35335](https://jira.mariadb.org/browse/MDEV-35335?filter=-2)|
| 8   | MariaDB    | MDEV-35464    | [MDEV-35464](https://jira.mariadb.org/browse/MDEV-35464?filter=-2)|
| 9   | MariaDB    | MDEV-36120    | [MDEV-36120](https://jira.mariadb.org/browse/MDEV-36120?filter=-2)|
| 10  | MariaDB    | MDEV-36308    | [MDEV-36308](https://jira.mariadb.org/browse/MDEV-36308?filter=-2)|
| 11  | MariaDB    | MDEV-36330    | [MDEV-36330](https://jira.mariadb.org/browse/MDEV-36330?filter=-2)|
| 12  | MariaDB    | MDEV-36358    | [MDEV-36358](https://jira.mariadb.org/browse/MDEV-36358?filter=-2)|
| 13  | OceanBase  | #2248         | [#2248](https://github.com/oceanbase/oceanbase/issues/2248)        |