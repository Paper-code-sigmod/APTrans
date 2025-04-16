#!/bin/bash

# 设置路径和参数
Generate_Path="./sqlancer"
Test_Isolation=("serializable" "repeatable_read" "read_committed")
# Test_Isolation=("repeatable_read")

# 构建sqlancer项目
echo "开始构建 sqlancer 项目..."
cd $Generate_Path
mvn clean package
if [ $? -ne 0 ]; then
  echo "构建 sqlancer 项目失败！"
  exit 1
fi
echo "构建成功！"

cd ..

for isolation in "${Test_Isolation[@]}"
do
  # 生成样本并运行SQLancer
  # java -jar $Generate_Path/target/sqlancer-2.0.0.jar --sample_type postgres --save_path ./cases/postgres/$isolation --clean_save_path true --sample_num 10 --test_isolation $isolation
  java -jar $Generate_Path/target/sqlancer-2.0.0.jar --sample_type mysql --save_path ./cases/mysql/$isolation --clean_save_path true --sample_num 300 --test_isolation $isolation
done

# 退出脚本
exit
