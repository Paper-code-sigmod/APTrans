import shutil
import pandas as pd
import re
import os
import pymysql
import time
from datetime import datetime

from db_config import DBConfig
from graph_class import *
from executor import Executor
from checker import Checker
from checker import Check_Error

def record_details(index, table_name, pattern, schedule, txn_sql, execute_message, order):
    if dbconfig.isolation_level == "READ COMMITTED":
        save_path = f'./check/READ_COMMITTED/{dbconfig.args.database_type}/{dbconfig.date}/{file_index}/'
    elif dbconfig.isolation_level == "REPEATABLE READ":
        save_path = f'./check/REPEATABLE_READ/{dbconfig.args.database_type}/{dbconfig.date}/{file_index}/'
    elif dbconfig.isolation_level == "SERIALIZABLE":
        save_path = f'./check/SERIALIZABLE/{dbconfig.args.database_type}/{dbconfig.date}/{file_index}/'
    txt_name = file_path.split("/")[-1]
    if not os.path.exists(save_path):
        os.makedirs(save_path)

    with open(f'{save_path}{txt_name}_{index}.txt', 'w') as f:
        f.write((",".join(table_name)) + "\n")
        f.write("pattern: \n" + pattern + "\n")
        f.write("schedule:\n" + str(schedule) + "\n")
        f.write("Txn:\n" + txn_sql + "\n")
        f.write("results:\n" + execute_message + "\n")
        f.write("order:\n" + (" ".join(order)) + "\n")
        if os.path.exists(file_path):
            shutil.copy(file_path, save_path)  # 拷贝文件到指定目录
            print(f"File {file_path} has been copied to {save_path}")
        else:
            print(f"File {file_path} does not exist, skipping copy.")
        print(f"——————Recorded details for file {txt_name} index {index}——————")


def Execute_Cases(dbconfig, data_list, table_name, executor):
    for case in data_list:
        iteration_start_time = time.time()  # 记录每次迭代的开始时间
        index = case.get("Case Num")
        pattern_id = int(case.get("Pattern Category"))
        pattern = case.get("Pattern")
        data_sql = case.get("Data SQL").strip('[]').split(';,')
        data_sql = [sql.strip() + ';' for sql in data_sql if sql.strip() != '']
        txn_sql = case.get("Transactions")
        matches = re.findall(r'\[.*?\]', txn_sql)
        txns = [[s.strip() + ';' for s in sql.strip('[]').split(';,') if s.strip() != ''] for sql in matches]
        schedule = [int(item) for item in case.get("Schedule").split(',')]

        # Data SQL Insert SQLs
        init_start_time = time.time()  # 记录初始化开始时间
        init_message_mysql = executor.execute_data_sql(data_sql)
        init_duration = time.time() - init_start_time  # 计算初始化耗时
        time_stats["initialization"] += init_duration
        # print(f"Initialization duration for index {index}: {init_duration:.2f} seconds")

        # 执行事务
        txn_start_time = time.time()  # 记录事务开始时间
        execute_message, execute_errors, sql_results, execute_order = executor.execute_transactions(txns, schedule, pattern)
        txn_duration = time.time() - txn_start_time  # 计算事务耗时
        execute_results = "".join(map(str, execute_message))
        execute_errors = "".join(map(str, execute_errors))
        time_stats["transaction_execution"] += txn_duration
        # print(f"Transaction execution duration for index ——[{index}]——[{pattern}]: {txn_duration:.2f} seconds")
        
        check_start_time = time.time()  # 记录检查开始时间
        # 处理模式检查
        print(f"Success Excute & Checking ——[{index}]——[{pattern}]")
        # print(sql_results)
        wrtie_sql = executor.nomalize_result("WRITE", execute_message, 1)
        sql_results.extend(wrtie_sql)
        
        exe_err = Check_Error(execute_errors)
        if exe_err:
            continue
        
        ecp = Checker(dbconfig, sql_results, execute_message, execute_order, executor)
        if ecp:
            # 记录详细信息
            record_details(index, table_name, pattern, schedule, txn_sql, execute_results, execute_order)

        check_duration = time.time() - check_start_time  # 计算检查耗时
        time_stats["pattern_check"] += check_duration
        # print(f"Check duration for index {index}: {check_duration:.2f} seconds")


if __name__ == "__main__":
    # 记录总开始时间
    dbconfig = DBConfig()
    executor = Executor(dbconfig)
    executor.Create_New_Database()
    executor.init_config()
    overall_start_time = time.time()
    # 初始化时间统计字典
    time_stats = {
        "initialization": 0,
        "transaction_execution": 0,
        "read_statement_execution": 0,
        "pattern_check": 0,
        "total": 0
    }

    # 处理文件
    path = dbconfig.args.cases_path
    # 遍历目录中的每个文件
    Table_Names = []
    file_index = 0
    for file in os.listdir(path):
        file_path = os.path.join(path, file)
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

                # 将数据整理为符合需求的格式, 确保所有需要的列都存在，即使数据中没有相应的键
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
                    if t_data == "":
                        continue
                    formatted_data = {
                        "Case Num": i,
                        "Pattern Category": data.get("VAR Num " + str(i), ""),
                        "Pattern": data.get("Pattern " + str(i), ""),
                        "Data SQL": data.get("Data SQL " + str(i), ""),
                        "Schedule": data.get("Schedule " + str(i), ""),
                        "Transactions": data.get("Case " + str(i), "")
                    }
                    data_list.append(formatted_data)
                Execute_Cases(dbconfig, data_list, Table_Names, executor)
        Table_Names.clear()
        executor.Table_Names.clear()

    # 计算总耗时
    time_stats["total"] = sum(time_stats.values())
    # 统计各部分时间占比
    print("Time statistics:")
    for key, value in time_stats.items():
        print(f"{key}: {value:.2f} seconds, proportion: {value / time_stats['total'] * 100:.2f}%")
    print("Total execution time:", time.time() - overall_start_time)