import concurrent.futures

import pymysql
import psycopg2
import time
import threading
import random
from datetime import datetime

from db_config import DBConfig
from db_config import Trigger

def getTime():
    return "\'" + datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f') + "\'"

class TransactionThread(threading.Thread):
    # 初始化时，传入事务ID和SQL语句
    # 每当被调度时，按顺序执行SQL语句
    def __init__(self, txn_id, conn, executor):
        threading.Thread.__init__(self)
        self.executor = executor
        self.txn_id = txn_id
        self.conn = conn
        self.cursor = self.conn.cursor()
        self.pending_statements = []
        self.result = []  # 事务执行结果, 返回受影响的行数
        self.block = False  # 是否被阻塞
        self.error_messages = [] # 错误信息
        self.read_results = []  # 读取结果
        self.finished_sql = ""

    def run(self):
        # 线程启动后，等待主线程调度
        while not self.finished:
            time.sleep(0.01)  # 防止空转占用CPU过高

    def execute_pending_statements(self):
        # 被调度时，选择一条SQL语句，放入执行队列
        # 用于主线程判断线程是否被阻塞
        if len(self.pending_statements) > 0:
            self.block = True
            self.error = False
            stmt = self.pending_statements[0]
            try:
                origin_sql = stmt
                stmt, table_num = self.executor.modify_sql(stmt)
                time = getTime()
                self.cursor.execute(stmt)
                if stmt.startswith("COMMIT"):
                    result = (self.txn_id, time)
                elif stmt.startswith("ABORT"):
                    result = (self.txn_id, time)
                elif stmt.startswith("BEGIN"):
                    result = "BEGIN"
                elif stmt.startswith("ROLLBACK"):
                    result = "ROLLBACK"
                elif stmt.startswith("START"):
                    result = "START"
                elif stmt.startswith("SELECT"):
                    result = self.cursor.fetchall()
                    read_result = self.executor.nomalize_result(stmt, result, table_num)
                    self.read_results.append(read_result)
                    result = len(result)
                else:
                    result = self.cursor.rowcount
                # print((origin_sql, result))
                self.result.append((origin_sql.replace(", INFO", ""), result))
            except Exception as e:
                error_message = f"Transaction {self.txn_id + 1}: Error {e}"
                # print((stmt, error_message))
                self.error_messages.append((stmt, error_message))
            finally:
                self.finished_sql = origin_sql
                self.pending_statements.pop(0)  # 移除已执行的SQL语句
                self.block = False

class Executor:
    def __init__(self, db_config):
        self.db_config = db_config
        self.Table_Names = []
    
    def create_connection(self, dbconfig = None):
        if dbconfig is None:
            dbconfig = self.db_config.config
        if self.db_config.DBType == 'mysql':
            return pymysql.connect(**dbconfig)
        else:
            return psycopg2.connect(**dbconfig)
    
    # 数据库参数配置
    def init_config(self):
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            # cur.execute("SET GLOBAL TRANSACTION ISOLATION LEVEL SERIALIZABLE")
            if self.db_config.args.database_type == 'oceanbase':
                cur.execute("SET ob_trx_lock_timeout = 1000")
            else:    
                cur.execute("SET GLOBAL innodb_lock_wait_timeout = 1")
                cur.execute("SET GLOBAL lock_wait_timeout = 1")
            cur.close()
            conn.commit()
            conn.close()
        except Exception as e:
            print(e)

    def Create_New_Database(self):
        database_config = self.db_config.config.copy()
        sql1 = f"DROP DATABASE IF EXISTS {database_config['database']};"
        sql2 = f"CREATE DATABASE {database_config['database']};"
        if self.db_config.DBType == 'mysql':
            database_config.pop('database')
        elif self.db_config.DBType == 'postgres':
            database_config['database'] = 'postgres'
        try:
            conn = self.create_connection(database_config)
            conn.autocommit = True
            cur = conn.cursor()
            cur.execute(sql1)
            cur.execute(sql2)
            cur.close()
            conn.close()
            print("Database created successfully")
        except Exception as e:
            error_message = f"{e}"
            print("Init Database Error: " + error_message)

    # 创建表
    def Create_Table(self, create_sql, table_name):
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            cur.execute("DROP TABLE IF EXISTS " + table_name)
            cur.execute(create_sql)
            trigger_table = Trigger.create_trigger_table(create_sql, table_name)
            for trigger_sql in trigger_table:
                cur.execute(trigger_sql)
            cur.close()
            conn.commit()
            conn.close()
            self.Table_Names.append(table_name)
        except Exception as e:
            error_message = f"Create Table Error: {e}"
            print(error_message)
            return error_message
        
    # 初始化触发器
    def Init_Trigger(self):
        triggers = Trigger(self.Table_Names, self.db_config.DBType)
        trigger_init = triggers.get_trigger_init()
        # 每一个表都关联一个触发器，使得当表被修改时，触发器记录相关的属性
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            for trigger_sql in trigger_init:
                cur.execute(trigger_sql)
            cur.close()
            conn.commit()
            conn.close()
        except Exception as e:
            error_message = f"INIT Trigger Error: {e}"
            print(error_message)
            return error_message
        
    def Get_Write_log(self):
        # Generate the queries you want to run for each table
        trigger_select_table = [
            f"SELECT operation_type, session_id, ID, VAL, time FROM {table_name}_log;"
            for table_name in self.Table_Names
        ]
        write_result = []

        try:
            # Connect to the database
            conn = self.create_connection()
            cur = conn.cursor()

            # Execute each query and collect the results
            for query in trigger_select_table:
                cur.execute(query)
                rows = cur.fetchall()
                       
                # Append rows to write_result
                write_result.extend(rows)

            return write_result
        except Exception as e:
            # Handle exceptions
            print(f"Error retrieving logs: {e}")
            return None

        finally:
            # Always close the connection
            if conn:
                conn.close()

    def check_delete_dependency(self, read_sql, print_middle_result=True):
        # 替换成触发器表名
        table_num = 0
        for table_name in self.Table_Names:
            if table_name in read_sql:
                read_sql = read_sql.replace(table_name, f"{table_name}_log")
                table_num = table_num + 1
        # read_sql = read_sql.replace("SELECT", "SELECT operation_type, ")
        idx = read_sql.find('WHERE')
        read_all = ''
        if idx == -1:
            read_all = read_sql
        else:
            read_all = read_sql[0:idx] + ';'
        if print_middle_result:
            print("check_delete_dependency : " + read_sql)
            print("check read : " + read_all)
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            cur.execute(read_sql)
            read_res = cur.fetchall()
            cur.execute(read_all)
            all_res = cur.fetchall()
            cur.close()
            conn.commit()
            conn.close()
        except Exception as e:
            error_message = f"Check Delete Dependency Error: {e}"
            print(error_message)
            return error_message
        
        all_id = self.nomalize_result("SELECT", all_res, table_num)
        all_id = [row[2] for row in all_id if row[2] is not None]
        
        read_id = self.nomalize_result("SELECT", read_res, table_num)
        read_id = [row[2] for row in read_id if row[2] is not None]

        diff_id = list(all_id)
        tmp_id = list(read_id)
        for e in all_id:
            if e in tmp_id:
                diff_id.remove(e)
                tmp_id.remove(e)
        print("filter_id: ", diff_id)
        print("read_id: ", read_id)
        res_id = list(set(read_id) - set(diff_id))
        return res_id
    
    # 执行读取函数，SELECT语句
    def execute_read_sql(self, sql):
        # print(sql)
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            cur.execute(sql)
            result = None
            if sql.startswith("SELECT"):
                result = cur.fetchall()
            cur.close()
            conn.commit()
            conn.close()
            return result
        except Exception as e:
            error_message = f"READ Error {sql}: {e} "
            print(error_message)
            return error_message     

    # 执行数据插入，INSERT语句
    def execute_data_sql(self, sql_statements):
        for sql in sql_statements:
            # 出现错误，跳过当前的这条语句
            sql = sql.replace(";;",";")
            try:
                conn = self.create_connection()
                cur = conn.cursor()
                cur.execute(sql)
                cur.close()
                conn.commit()
                conn.close()
            except Exception as e:
                error_message = f" INSERT Error: {e}"
                if 'Duplicate entry' in error_message:
                    continue
                print("Init:" + sql + " " + error_message)
        trigger_delete_table = [
            f"DELETE FROM {table_name}_log;"
            for table_name in self.Table_Names
        ]
        for trigger_sql in trigger_delete_table:
            self.execute_read_sql(trigger_sql)

    # 修改select参数，使得返回中间结果
    def modify_sql(self, sql):
        sql = sql.replace(";;",";")
        if sql.startswith("SELECT"):
            tables = 0
            # 生成INFO常量
            info_string = ""
            # 查看操作了哪些表, 生成 id0, val0, id1, val1, ...
            for table in self.Table_Names:
                if table in sql:
                    info_string = info_string + f"{table}.ID AS id{self.Table_Names.index(table)}, {table}.VAL AS val{self.Table_Names.index(table)}, "
                    tables = tables + 1
            # 记录CONNECT ID
            if self.db_config.DBType == 'mysql':
                info_string = info_string + "CONNECTION_ID() AS connect_id, "
                info_string = info_string + "SYSDATE(6) AS time"
            elif self.db_config.DBType == 'postgres':
                info_string = info_string + "pg_backend_pid() AS connect_id, "
                info_string = info_string + "clock_timestamp()::timestamp without time zone AS time"
            # 替换INFO
            return sql.replace("INFO", info_string), tables
        # 使用触发器时，写操作由触发器直接记录。
        elif sql.startswith("UPDATE") and self.db_config.DBType == 'mysql':
            for table in self.Table_Names:
                target_str = f"UPDATE {table}"
                if target_str in sql:
                    sql = sql.replace("VAL", f"{table}.VAL")
                    break
        return sql, 1

    def nomalize_result(self, stmt, result, table_num):
        if stmt.startswith("SELECT"):
            read_results = []
            for row in result:
                r_row = row[-(table_num * 2 + 2):]  # 截取最后table_num*2+2个元素
                for i in range(table_num):
                    data_op = "SELECT"
                    data_id = r_row[i * 2]
                    data_val = r_row[i * 2 + 1]
                    data_connect = r_row[-2]
                    data_time = r_row[-1]
                    read_results.append((data_op, data_connect, data_id, data_val, data_time, stmt))
            return read_results
        else:
            write_results = []
            result = self.Get_Write_log()
            for row in result:
                data_op = row[0]
                data_connect = row[1]
                data_id = row[2]
                data_val = row[3]
                data_time = row[4]
                write_results.append((data_op, data_connect, data_id, data_val, data_time, stmt))
                # print((stmt, data_op, data_connect, data_id, data_val, data_time))
            return write_results

    # 执行事务函数
    def execute_transactions(self, transactions, schedule, pattern, print_middle_result=True):
        execution_results = []
        read_results = []
        error_messages = []
        indices = [0] * len(transactions)
        stmts_state = [0] * len(schedule)
        stmts_order = []
        wait_threads = []
        sql_pattern = {}
        # 注释跑
        # print("Original Schedule: ", shcedule)
        # first_two = schedule[:2]  # 前两个元素
        # rest_of_schedule = schedule[2:]  # 剩余元素
        # random.shuffle(rest_of_schedule)
        # schedule = first_two + rest_of_schedule
        # print("Shuffle Schedule: ", shcedule)
        execution_order = []

        # 生成根据调度序列生成语句序列
        pat_idx = 0
        for s in schedule:
            txn_id = s - 1
            sql = transactions[txn_id][indices[txn_id]]
            stmts_order.append(sql)
            indices[txn_id] += 1

        with concurrent.futures.ThreadPoolExecutor() as executor:
            txn_threads = []
            futures = []
            connections = [self.create_connection() for _ in transactions]
            
            # 初始化事务线程
            for txn_id, txn in enumerate(transactions):
                conn = connections[txn_id]
                cursor = conn.cursor()
                
                if self.db_config.DBType == "mysql":
                    isolation_level_query = "SET SESSION TRANSACTION ISOLATION LEVEL " + self.db_config.isolation_level + ";"
                    cursor.execute(isolation_level_query)
                elif self.db_config.DBType == "postgres":
                    # SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL SERIALIZABLE;
                    isolation_level_query = "SET TRANSACTION ISOLATION LEVEL " + self.db_config.isolation_level + ";"
                    cursor.execute(isolation_level_query)
                else:
                    assert False, 'DBType not support'
                
                cursor.execute(self.db_config.get_session_query())
                session_id = cursor.fetchone()[0]

                cursor.close()
                
                txn_thread = TransactionThread(session_id, conn, self)
                txn_threads.append(txn_thread)
            try:
                while sum(stmts_state) < len(schedule) and len(error_messages) == 0:
                    for i, stmt in enumerate(stmts_order):
                        # print(stmts_state)
                        # print("Testing" , i, stmt)
                        txn_id = schedule[i] - 1

                        # 判断该语句是否已提交，且事务是否阻塞
                        if stmts_state[i] == 1 or txn_threads[txn_id].block == True:
                            continue

                        txn_thread = txn_threads[txn_id]
                        txn_thread.pending_statements.append(stmt)
                        
                        future = executor.submit(txn_thread.execute_pending_statements)
                        futures.append(future)
                    
                        stmts_state[i] = 1

                        # 如果事务未完成，标记阻塞状态
                        time.sleep(0.1)
                        # 优化提交操作的情况
                        if stmt.startswith("COMMIT") and txn_thread.block == True:
                            time.sleep(1)

                        if txn_thread.block == True:
                            wait_threads.append(txn_thread)
                            execution_order.append(f"{txn_thread.txn_id}b")
                            # execution_order.append(sql_pattern[str(txn_thread.txn_id) + stmt] + "b")
                            break

                        # 如果事务完成，收集结果
                        if len(txn_thread.error_messages) > 0:
                            error_messages.extend(txn_thread.error_messages)
                            break
                        if len(txn_thread.read_results) > 0:
                            read_res = txn_thread.read_results.pop(0)
                            read_results.extend(read_res)
                        res = txn_thread.result.pop(0)
                        execution_results.append(res)
                        execution_order.append(f"{txn_thread.txn_id}")
                        # execution_order.append(sql_pattern[str(txn_thread.txn_id) + stmt])

                        # 检查是否有已被释放的阻塞事务
                        for t in wait_threads[:]:
                            if len(t.pending_statements) == 0:
                                if len(t.error_messages) > 0:
                                    error_messages.extend(t.error_messages)
                                    break
                                if len(t.read_results) > 0:
                                    read_res = t.read_results.pop(0)
                                    read_results.extend(read_res)
                                res = t.result.pop(0)
                                execution_results.append(res)
                                execution_order.append(f"{t.txn_id}")
                                # execution_order.append(sql_pattern[str(t.txn_id) + t.finished_sql])
                                wait_threads.remove(t)
                        break
                
                # 等待所有事务执行完成
                for future in concurrent.futures.as_completed(futures):
                    future.result()  # 处理每个线程的结果
            # except Exception as e:
            #     print(f"Transaction execution failed: {e}")
            finally:
                for conn in connections:
                    conn.close()
        if print_middle_result:
            print(execution_order)
        return execution_results, error_messages, read_results, execution_order
    
    def Drop_Table(self):
        try:
            conn = self.create_connection()
            cur = conn.cursor()
            tables = ",".join(table_names)
            sql = f"DROP TABLE IF EXISTS {tables}"
            cur.execute()
            cur.close()
            conn.close()
            print(f"Tables {tables} dropped successfully")
        except Exception as e:
            error_message = f"Drop Table Error: {e}"
            return error_message