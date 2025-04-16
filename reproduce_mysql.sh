#!/bin/bash

#Test_Isolation="READ_COMMITTED"
#Test_Date="03-24"
#Test_Isolation="REPEATABLE_READ"
#Test_Date="03-25"
Test_Isolation="SERIALIZABLE"
Test_Date="04-14"

#Database="mysql"
#Host="172.17.0.2"
#Database="mariadb"
#Host="172.17.0.3"
Database="oceanbase"
Host="127.0.0.1"


# 设置路径和其他参数
Generate_Path="./sqlancer"
sample_type="${Database}_bugs"
cases_path="./check/$sample_type/$Test_Isolation/$Database/$Test_Date"
test_database="test_$Test_Isolation"

mkdir -p "reproduce-out/"
output_file="reproduce-out/$sample_type-$Test_Isolation.out"

echo "$Database-$Test_Isolation-$Test_Date"
nohup python3 ./executor/mysql_reproduce.py \
    --host $Host \
    --password "" \
    --port 12881 \
    --database_type "$Database" \
    --database "$test_database" \
    --cases_path "$cases_path" \
    --isolation "$Test_Isolation" \
    --reproduce "true" \
    > "$output_file"  &

#python3 ./executor/mysql_reproduce.py \
#    --host $Host \
#    --password "" \
#    --port 3306 \
#    --database_type "$Database" \
#    --database "$test_database" \
#    --cases_path "$cases_path" \
#    --isolation "$Test_Isolation" \
#    --reproduce "true"

#python3 ./executor/bugs_summary.py \
#  --host $Host \
#  --password "" \
#  --port 3306 \
#  --database_type "$Database" \
#  --database "$test_database" \
#  --cases_path "$cases_path" \
#  --isolation "$Test_Isolation" \
#  --reproduce "true"

# 检查 Python 脚本是否成功执行
if [ $? -ne 0 ]; then
    echo "错误: 执行 mysql_check.py 脚本失败。"
    exit 1
fi

# 退出脚本
#echo "mysql_check.py success"
exit 0
