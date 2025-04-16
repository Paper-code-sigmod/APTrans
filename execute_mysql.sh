#!/bin/bash

# 用法说明函数
usage() {
    echo "help: $0 -i <Test_Isolation> -d <Database>"
    echo "  -i    isolation_level(eg. serializable, repeatable_read, read_committed)"
    echo "  -d    database (eg. mysql, mariadb, oceanbase)"
    exit 1
}

# 初始化变量
Test_Isolation=""
Database=""

# 解析命令行选项
while getopts ":i:d:" opt; do
    case ${opt} in
        i )
            Test_Isolation=$OPTARG
            ;;
        d )
            Database=$OPTARG
            ;;
        \? )
            echo "无效的选项: -$OPTARG" 1>&2
            usage
            ;;
        : )
            echo "选项 -$OPTARG 需要一个参数" 1>&2
            usage
            ;;
    esac
done
shift $((OPTIND -1))

# 检查是否提供了所有必需的参数
if [ -z "$Test_Isolation" ] || [ -z "$Database" ]; then
    echo "error: not found Test_Isolation and Database "
    usage
fi

# 设置路径和其他参数
Generate_Path="./sqlancer"
sample_type="mysql"
cases_path="./cases/$sample_type/$Test_Isolation"
test_database="test_$Test_Isolation"

# 执行 mysql_check 脚本并捕获返回值
python3 ./executor/mysql_check.py \
    --host 127.0.0.1 \
    --password 123456 \
    --database_type "$Database" \
    --database "$test_database" \
    --cases_path "$cases_path" \
    --isolation "$Test_Isolation"

# 检查 Python 脚本是否成功执行
if [ $? -ne 0 ]; then
    echo "错误: 执行 mysql_check.py 脚本失败。"
    exit 1
fi

# 退出脚本
echo "mysql_check.py success"
exit 0
