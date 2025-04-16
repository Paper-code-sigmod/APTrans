import re
import os
import time
from os import mkdir

from db_config import DBConfig
from executor import Executor
from checker import Checker
from checker import Check_Error


def Execute_Cases(dbconfig, data_list, table_name, executor, txn_id):
    global total_record_bug_num, total_true_bug_num
    txn_num = len(txn_id)

    for case in data_list:
        if txn_num == 0:
            break
        iteration_start_time = time.time()  # 记录每次迭代的开始时间
        index = case.get("Case Num")
        pattern_id = int(case.get("Pattern Category"))
        pattern = case.get("Pattern")

        print_middle_result = False

        if index in txn_id:
            txn_num = txn_num - 1
            total_record_bug_num = total_record_bug_num + 1;
            print("")
            if pattern in pattern_stats:
                pattern_stats[pattern] += 1
            else:
                pattern_stats[pattern] = 1
                print_middle_result = True


        data_sql = case.get("Data SQL").strip('[]').split(';,')
        data_sql = [sql.strip() + ';' for sql in data_sql if sql.strip() != '']
        txn_sql = case.get("Transactions")
        matches = re.findall(r'\[.*?\]', txn_sql)
        txns = [[s.strip() + ';' for s in sql.strip('[]').split(';,') if s.strip() != ''] for sql in matches]
        schedule = [int(item) for item in case.get("Schedule").split(',')]

        if index in txn_id:
            with open(f'{pattern_sql_path}', 'a') as f:
                f.write(f"---Transaction SQL Begin !!!---\n")
                f.write(f"Pattern: {pattern}\n")
                sql_str = ""
                for txn in txns:
                    for sql in txn:
                        sql_str += sql
                f.write(f"SQL: {sql_str}\n")
                f.write("---Transaction SQL End !!!---\n")

        # Data SQL Insert SQLs
        init_start_time = time.time()  # 记录初始化开始时间
        init_message_mysql = executor.execute_data_sql(data_sql)
        init_duration = time.time() - init_start_time  # 计算初始化耗时
        time_stats["initialization"] += init_duration
        # print(f"Initialization duration for index {index}: {init_duration:.2f} seconds")

        # 执行事务
        txn_start_time = time.time()  # 记录事务开始时间
        execute_message, execute_errors, sql_results, execute_order = executor.execute_transactions(txns, schedule, pattern, print_middle_result)
        txn_duration = time.time() - txn_start_time  # 计算事务耗时
        execute_errors = "".join(map(str, execute_errors))
        time_stats["transaction_execution"] += txn_duration
        
        check_start_time = time.time()  # 记录检查开始时间
        if print_middle_result:
            print(f"Success Excute & Checking ——[{index}]——[{pattern}]")

        wrtie_sql = executor.nomalize_result("WRITE", execute_message, 1)
        sql_results.extend(wrtie_sql)
        
        exe_err = Check_Error(execute_errors)
        if exe_err:
            continue
        
        ecp = Checker(dbconfig, sql_results, execute_message, execute_order, executor, print_middle_result)
        if ecp and index in txn_id:
            total_true_bug_num = total_true_bug_num + 1

        check_duration = time.time() - check_start_time  # 计算检查耗时
        time_stats["pattern_check"] += check_duration


if __name__ == "__main__":
    # 记录总开始时间
    dbconfig = DBConfig()
    executor = Executor(dbconfig)
    executor.Create_New_Database()
    executor.init_config()
    overall_start_time = time.time()
    print_middle_result = True
    total_record_bug_num = 0
    total_true_bug_num = 0
    # 初始化时间统计字典
    time_stats = {
        "initialization": 0,
        "transaction_execution": 0,
        "read_statement_execution": 0,
        "pattern_check": 0,
        "total": 0
    }

    path = dbconfig.args.cases_path
    Table_Names = []
    file_index = 0
    process_all = True
    pattern_stats = {}
    os.makedirs(f"./reproduce-out/{dbconfig.args.database_type}-pattern-sql", exist_ok=True)
    pattern_sql_path = f"./reproduce-out/{dbconfig.args.database_type}-pattern-sql/{dbconfig.args.isolation.upper()}.txt"
    if os.path.exists(pattern_sql_path):
        os.remove(pattern_sql_path)

    for root, _, files in os.walk(path):
        if not process_all:
            user_input = input("Do you want to continue? (0 to quit, 9 to process all, other to continue one by one): ")
            if user_input == "0":
                break
            elif user_input == "9":
                process_all = True

        # 获取原始事务文件
        txn_id = []
        file_path = ""
        for file in files:
            if ".txt_" in file:
                file_id = int(file.split(".txt_")[1].split(".txt")[0])
                txn_id.append(file_id)
            else:
                file_path = os.path.join(root, file)
        txn_id.sort()
        data_list = []
        if os.path.isfile(file_path):
            file_index += 1
            with open(file_path, 'r', encoding='utf-8') as f:
                # 数据读取
                print(f'——————Read file: {file_path}——————')
                data = {}
                for line in f:
                    if line == '\n':
                        continue
                    # 将每行按 ': ' 分割为键和值
                    key, value = line.strip().split(': ', 1)
                    data[key] = value

                # Create table:
                table_num = int(data.get("Tables Num", 0))
                if table_num == 0:
                    print("TABLE NUM = 0")
                else:
                    for i in range(table_num):
                        Table_Names.append(data.get("Table " + str(i), ""))
                        Create_sql = data.get("Create SQL " + str(i), "")
                        executor.Create_Table(Create_sql, Table_Names[-1])

                # Init Trigger
                executor.Init_Trigger()
                # Case
                Case_num = int(data.get("Case Num", 0))
                if Case_num == 0:
                    print("CASE NUM = 0")
                    continue
                for i in range(Case_num):
                    t_data = data.get("VAR Num " + str(i), "")
                    if t_data == "": continue
                    formatted_data = {
                        "Case Num": i,
                        "Pattern Category": data.get("VAR Num " + str(i), ""),
                        "Pattern": data.get("Pattern " + str(i), ""),
                        "Data SQL": data.get("Data SQL " + str(i), ""),
                        "Schedule": data.get("Schedule " + str(i), ""),
                        "Transactions": data.get("Case " + str(i), "")
                    }
                    data_list.append(formatted_data)
                Execute_Cases(dbconfig, data_list, Table_Names, executor, txn_id)
        Table_Names.clear()
        executor.Table_Names.clear()

    print(f"Isolation level: {dbconfig.isolation_level}, Pattern statistics:")
    for pattern, count in pattern_stats.items():
        print(f"{pattern}: {count}")
    print("Total record bug num: ", total_record_bug_num)
    print("Total true bug num: ", total_true_bug_num)