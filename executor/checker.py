from graph_class import *
import pandas as pd
from db_config import Pattern
import db_config

def Check_Cycle(operation_groups, val_dict, executor, isolation, print_middle_result=True):
    graph = DirectedGraph()
    # 依次判断相邻之间的两个操作
    for current_op in operation_groups:
        for next_op in operation_groups:
            if current_op.operation_type == 'COMMIT' or next_op.operation_type == 'COMMIT':
                continue
            if current_op.time >= next_op.time:
                continue
            if current_op.txn_id == next_op.txn_id:
                continue
            
            # print(type(current_op.txn_id), type(next_op.txn_id))
            # print(current_op.txn_id, next_op.txn_id, (current_op.txn_id == next_op.txn_id))
            # 处理操作逻辑
            if current_op.operation_type == 'SELECT' and next_op.operation_type == 'SELECT':
                continue
            elif current_op.operation_type == 'SELECT':
                if isolation == 'READ COMMITTED':
                    continue
                if rw_dependency(current_op, next_op):
                    graph.add_edge(current_op.txn_id, next_op.txn_id, "rw")
                    if print_middle_result:
                        print(f"rw dependency: {current_op.id} -> {next_op.id}")
                else:
                    continue
            elif next_op.operation_type == 'SELECT':
                if wr_dependency(current_op, next_op, executor, print_middle_result):
                    graph.add_edge(current_op.txn_id, next_op.txn_id, "wr")
                    if print_middle_result:
                        print(f"wr dependency: {current_op.id} -> {next_op.id}")
                else:
                    continue
            else:
                if ww_dependency(current_op, next_op):
                    graph.add_edge(current_op.txn_id, next_op.txn_id, "ww")
                    if print_middle_result:
                        print(f"ww dependency: {current_op.id} -> {next_op.id}")
    
    error = False
    if (graph.edge_type["wr"] > 0) and isolation != 'READ COMMITTED':
        if print_middle_result:
            print("wr or ww dependency")
            error = True
    
    cycle = graph.has_cycle()
    if print_middle_result:
        print(f"Whether there is a cycle: {cycle}")
    
    error = error or cycle
    if error:
        graph.display(print_middle_result)
    return error

def Check_Value(operation_groups, isolation):
    value_dict = {}
    op_idx = 0
    while (op_idx < len(operation_groups)):
        if operation_groups[op_idx].operation_type == 'COMMIT':
            # Get the transaction id from the execute_op and update history to committed
            txn_id_to_commit = operation_groups[op_idx].txn_id  # Assuming the second element is txn_id
            for key in value_dict:
                # Iterate through all keys in value_dict
                value_commit = (0,0,0,0)
                for idx, entry in enumerate(value_dict[key]):
                    # If the transaction id matches and it's not already committed
                    # print(entry)
                    if entry[2] == txn_id_to_commit and not entry[3]:
                        # Update the committed flag to True
                        # log new version _ delete lost version
                        value_dict[key][idx] = (entry[0], entry[1], entry[2], True)
                        value_commit = (entry[0], entry[1], entry[2], True)
                if value_commit != (0,0,0,0):
                    # If the transaction id matches and it's not already committed
                    # log new version _ delete lost version
                    value_dict[key].append(value_commit)
            op_idx = op_idx + 1  # Move to the next execute operation
            continue
            
        operation = operation_groups[op_idx]
        key_id = operation.ids
        key_value = operation.vals
        key_num = len(key_id)
        op = operation.operation_type
        txn_id = operation.txn_id
        for idx in range(key_num):
            key = key_id[idx]
            val_history = []
            val_history.append((key_value[idx], op, txn_id, False)) # value, operation, txn_id, iscommitted, pattern
            if key in value_dict:
                value_dict[key].extend(val_history)
                val_his = value_dict[key]
                # 说明该行之前被读写过，去判断该次读写是否符合规则
                tar = len(val_his) - 2
                while (tar >= 0):
                    if op == "SELECT":
                        if isolation == "READ COMMITTED":
                            # 读到已提交的写入
                            if val_his[tar][3] == True and val_his[tar][1] != "SELECT":
                                if key_value[idx] != val_his[tar][0]:
                                    print(f"SELECT RC {key}: {key_value[idx]} != {val_his[tar][0]}")
                                    return False, value_dict
                                break
                            if val_his[tar][2] == txn_id:
                                # 读到自身未提交的写入
                                if key_value[idx] != val_his[tar][0]:
                                    print(f"SELECT RCU {key}: {key_value[idx]} != {val_his[tar][0]}")
                                    return False, value_dict
                                break
                            
                        else:      
                            if val_his[tar][2] == txn_id:
                                if key_value[idx] != val_his[tar][0]:
                                    print(f"SELECT {key}: {key_value[idx]} != {val_his[tar][0]}")
                                    return False, value_dict
                                break
                    else:
                        if val_his[tar][3] != True and val_his[tar][2] != txn_id and val_his[tar][1] != "SELECT":
                            print(f"WRITE {key}: {val_his[tar][0]} , {key_value[idx]}")
                            return False, value_dict
                    tar -= 1
            else:
                value_dict[key] = val_history
        op_idx += 1
    return True, value_dict


def Checker(config, read_results, execute_message, order ,executor, print_middle_result=True):
    data_columns = ['operation', 'txn_id', 'id', 'val', 'time', 'sql']
    data = pd.DataFrame(read_results, columns=data_columns)

    if len(data) == 0:
        if print_middle_result:
            print("No Data To Check")
        return False
    if print_middle_result:
        print(execute_message)
    for stmt, message in execute_message:
        if stmt.startswith("COMMIT") or stmt.startswith("ROLLBACK"):
            data.loc[len(data)] = ['COMMIT', message[0], 0, 0, message[1], stmt]
    # 将整个 DataFrame 按时间戳排序
    data['time'] = pd.to_datetime(data['time'])
    data = data.sort_values(by='time').reset_index(drop=True)
    # 生成实际执行的调度序列
    # 首先规整数据
    for idx in range(1, len(data)):
        prev = data.iloc[idx - 1]
        curr = data.iloc[idx]
        if curr['txn_id'] == prev['txn_id'] and curr['operation'] == prev['operation']:
            data.loc[idx, 'time'] = data.loc[idx - 1, 'time']
    data = data.drop_duplicates()
    pd.set_option('display.max_rows', None)
    
    # 筛选出所有的INSERT和DELETE操作
    insert_rows = data[data['operation'] == 'INSERT'][['txn_id', 'id']]
    delete_rows = data[data['operation'] == 'DELETE'][['txn_id', 'id']]

    # 为了删除那些在相同txn_id下被DELETE的INSERT，创建一个删除条件
    insert_condition = insert_rows[['txn_id', 'id']]

    # 根据相同的txn_id和id从原始数据中删除对应的INSERT行
    df_cleaned = data[~((data['operation'] == 'DELETE') & 
                    data[['txn_id', 'id']].apply(tuple, axis=1).isin(insert_condition.apply(tuple, axis=1)))]
    data = df_cleaned

    if print_middle_result:
        print(data.iloc[:,:-1])
    
    operation_groups = []
    op_id = 0
    # 按照时间戳, 操作，事务进行分组
    for timestamp, group in data.groupby(['time', 'operation', 'txn_id']):
        # 获取分组中的特定列
        operation_type = group['operation'].iloc[0]  # 获取操作类型
        txn_id = group['txn_id'].iloc[0]  # 获取事务 ID
        sql = group['sql'].iloc[0]  # 获取 SQL 语句
        ids = group['id'].tolist()  # 获取 id 列并转换为列表
        vals = group['val'].tolist()  # 获取 val 列并转换为列表
        
        # 创建 OperationGroup 对象
        operation = OperationGroup(op_id, sql, timestamp, txn_id, operation_type, ids, vals)
        operation_groups.append(operation)
        
        # 增加操作 ID
        op_id += 1
    if print_middle_result:
        print(operation_groups)

    # 检测查询的值是否正确
    val_ecp, val_dict = Check_Value(operation_groups, config.isolation_level)
    if print_middle_result:
        print(val_dict)
    if not val_ecp:
        return True
    dep_ecp = Check_Cycle(operation_groups, val_dict, executor, config.isolation_level, print_middle_result)
    return dep_ecp

def Check_Error(error_message):
    # print(error_message)
    # 检查执行错误，要细分错误类型：TODO IN experiment
    Error = False
    if 'Lock wait' in error_message:
        Error = True
    elif 'Duplicate entry' in error_message:
        Error = True
    elif 'duplicate' in error_message:
        Error = True
    elif 'Record has changed' in error_message:
        Error = True
    elif 'not serialize' in error_message:
        Error = True
    elif 'Deadlock found' in error_message:
        Error = True
    else:
        if error_message != "":
            print(f"Errors: {error_message}")
    return Error