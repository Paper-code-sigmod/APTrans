#!/bin/bash

#Test_Isolation="READ_COMMITTED"
#Test_Isolation="REPEATABLE_READ"
Test_Isolation="SERIALIZABLE"

#Database="mysql"
#Database="mariadb"
Database="oceanbase"


# 设置路径和其他参数
Generate_Path="./sqlancer"
sample_type="${Database}_bugs"
cases_path="./check/$sample_type/$Test_Isolation/$Database/$Test_Date"
test_database="test_$Test_Isolation"

mkdir -p "reproduce-out/"
output_file="reproduce-out/$sample_type-$Test_Isolation.out"

python3 ./executor/bugs_summary.py \
  --database_type "$Database" \
  --cases_path "$cases_path" \
  --isolation "$Test_Isolation" \
  --reproduce "true" \
  --unique_rate 0.5

# 检查 Python 脚本是否成功执行
if [ $? -ne 0 ]; then
    echo "错误: 执行 mysql_check.py 脚本失败。"
    exit 1
fi

# 退出脚本
#echo "mysql_check.py success"
exit 0
