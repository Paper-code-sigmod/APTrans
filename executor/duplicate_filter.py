import os
import re

def filter_select_statements(input_file, output_file):
    """
    过滤文件中的SELECT语句，筛选出符合四种模式的语句，并保存到输出文件。
    
    :param input_file: 输入文件路径
    :param output_file: 输出文件路径
    """
    # 定义四种目标匹配模式
    pattern1 = r"SELECT\s+(-?\d+):\s*(-?\d+)\s+!=\s+(-?\d+)"
    pattern2 = r"SELECT RC\s+(-?\d+):\s*(-?\d+)\s+!=\s+(-?\d+)"
    pattern3 = r"SELECT RCPU\s+(-?\d+):\s*(-?\d+)\s+!=\s+(-?\d+)"
    pattern4 = r"WRITE\s+(-?\d+):\s*(-?\d+)\s+!=\s+(-?\d+)"

    # 打开输入文件，读取内容
    with open(input_file, 'r') as f:
        content = f.readlines()

    # 创建一个列表存储筛选出的行
    filtered_lines = []
    
    # 遍历每一行，查找符合条件的语句
    for line in content:
        # 检查是否匹配任何一个模式
        match = re.search(pattern1, line) or re.search(pattern2, line) or re.search(pattern3, line) or re.search(pattern4, line)
        
        if match:
            # 提取匹配到的三个整数
            num1, num2, num3 = int(match.group(1)), int(match.group(2)), int(match.group(3))
            # 添加符合条件的行
            filtered_lines.append(line)

    filtered_lines = list(set(filtered_lines))  # 去重
    return filtered_lines

def filter_transactions(input_file, output_file):
    """
    过滤文件中的事务记录，筛选出符合 {'rw': <整数>, 'ww': 0, 'wr': 1} 的数据，并保存到输出文件。
    
    :param input_file: 输入文件路径
    :param output_file: 输出文件路径
    """
    # 定义目标匹配模式，匹配整数值
    pattern = r"\{'rw':\s*(-?\d+),\s*'ww':\s*0,\s*'wr':\s*1\}"

    # 打开输入文件，读取内容
    with open(input_file, 'r') as f:
        content = f.readlines()

    # 创建一个列表存储筛选出的行
    filtered_lines = []
    
    # 遍历每一行，查找符合条件的记录
    for line in content:
        # 查找符合模式的部分
        if re.search(pattern, line):
            filtered_lines.append(line)

    filtered_lines = list(set(filtered_lines))  # 去重
    return filtered_lines

def process_logs_in_directory(input_dir, output_file):
    """
    遍历指定文件夹中的所有日志文件，分析每个文件并输出结果到同一个输出文件。
    
    :param input_dir: 输入文件夹路径
    :param output_file: 输出文件路径
    """
    # 打开输出文件，以写模式打开
    with open(output_file, 'w') as output_f:
        # 遍历目录中的每个文件
        for filename in os.listdir(input_dir):
            input_file_path = os.path.join(input_dir, filename)

            # 只处理以 .log 结尾的文件
            if os.path.isfile(input_file_path) and filename.endswith(".log"):
                # 添加文件说明
                output_f.write(f"\n--- 结果来自文件: {filename} ---\n")
                
                # 分别调用两个过滤函数
                select_results = filter_select_statements(input_file_path, output_file)
                transaction_results = filter_transactions(input_file_path, output_file)

                # 写入SELECT语句结果
                if select_results:
                    output_f.write("\n# 选择的SELECT语句：\n")
                    output_f.writelines(select_results)
                else:
                    output_f.write("\n# 没有找到符合的SELECT语句。\n")

                # 写入事务记录结果
                if transaction_results:
                    output_f.write("\n# 选择的事务记录：\n")
                    output_f.writelines(transaction_results)
                else:
                    output_f.write("\n# 没有找到符合的事务记录。\n")
                
                # 为下一个文件留一个空行
                output_f.write("\n")

    print(f"过滤完成，所有结果已保存至 {output_file}")

# 使用示例
input_dir = '/apt/log/'  # 输入文件夹路径
output_file = '/apt/filtered_logs_all_results.log'  # 输出文件路径
process_logs_in_directory(input_dir, output_file)
