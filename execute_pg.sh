#!/bin/bash

# 用法说明函数
usage() {
    echo "help: $0 -i <Test_Isolation> -d <Database>"
    echo "  -i    isolation_level (eg serializable, repeatable_read, read_committed)"
    echo "  -d    database (eg postgres, opengauss, tdsql)" 
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
sample_type="postgres"
cases_path="./cases/$sample_type/$Test_Isolation"
test_database="test_$Test_Isolation"

# 根据 Database 参数选择相应的 Python 脚本
case "$Database" in
    postgres)
        Python_Script="./executor/pg_check.py --user postgres --database_type postgres --database $test_database --cases_path $cases_path --isolation $Test_Isolation"
        ;;
    tdsql)
        Python_Script="./executor/pg_check.py --user tbase_test --password tbase123 --database_type tdsql --database $test_database --cases_path $cases_path --isolation $Test_Isolation"
        ;;
    opengauss)
        Python_Script="./executor/pg_check.py --user gaussdb --password Enmo@123 --database_type opengauss --database $test_database --cases_path $cases_path --isolation $Test_Isolation"
        ;;
    *)
        echo "错误: 不支持的数据库类型 '$Database'。"
        usage
        ;;
esac

# 执行相应的 Python 脚本并记录日志
python3 $Python_Script

# 检查 Python 脚本是否成功执行
if [ $? -ne 0 ]; then
    echo "错误: 执行脚本失败。"
    exit 1
fi

# 退出脚本
echo "success"
# 退出脚本
exit 0
