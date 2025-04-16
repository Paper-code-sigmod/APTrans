import re

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from db_config import DBConfig


def replace_keywords_with_numbers(sql, key_words_dict):
    sorted_keywords = sorted(key_words_dict.keys(), key=len, reverse=True)
    pattern = re.compile(r'\b(' + '|'.join(re.escape(word) for word in sorted_keywords) + r')\b', re.IGNORECASE)
    matches = pattern.findall(sql.upper())
    sequence = [key_words_dict[match] for match in matches]
    return sequence


def calculate_cosine_similarity(seq1, seq2):
    max_len = max(len(seq1), len(seq2))
    seq1_padded = seq1 + [0] * (max_len - len(seq1))
    seq2_padded = seq2 + [0] * (max_len - len(seq2))
    seq1_array = np.array(seq1_padded).reshape(1, -1)
    seq2_array = np.array(seq2_padded).reshape(1, -1)
    similarity = cosine_similarity(seq1_array, seq2_array)[0][0]
    return similarity


if __name__ == "__main__":
    dbconfig = DBConfig()
    pattern_sql_path = f"./reproduce-out/{dbconfig.args.database_type}-pattern-sql/{dbconfig.args.isolation.upper()}.txt"

    with open(pattern_sql_path, "r") as f:
        input_string = f.read()

    pattern_regex = r"---Transaction SQL Begin !!!---\s*Pattern:\s*(.*?)\s*SQL:\s*(.*?)\s*---Transaction SQL End !!!---"
    matches = re.findall(pattern_regex, input_string, re.DOTALL)

    key_words = ["SELECT", "FROM", "WHERE", "AND", "OR", "JOIN", "CROSS JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "ORDER BY", "FOR UPDATE", "UPDATE", "SET", "INSERT INTO", "VALUES", "DELETE", "=", ">", "<", ">=", "<=", "IN", "BETWEEN", "BEGIN", "COMMIT", "ROLLBACK"]
    key_words_dict = {key: i for i,  key in enumerate(key_words)}

    pattern_sql_dict = {}
    pattern_stats = {}
    for i, match in enumerate(matches, start=1):
        pattern, sql = match
        sql_squence = replace_keywords_with_numbers(sql, key_words_dict)
        if pattern not in pattern_sql_dict:
            pattern_sql_dict[pattern] = []
        pattern_sql_dict[pattern].append(sql_squence)

    print("————————Pattern Unique Statistics————————")
    total_num = 0
    total_unique_num = 0
    for pattern, sql_list in pattern_sql_dict.items():
        total_num += len(sql_list)
        unique_num = len(sql_list)
        flag = [0 for _ in range(len(sql_list))]
        for i, sql1 in enumerate(sql_list):
            if flag[i] == 1: continue
            for j, sql2 in enumerate(sql_list):
                if i >= j or flag[j]: continue
                similarity = calculate_cosine_similarity(sql1, sql2)
                if similarity > dbconfig.args.unique_rate:
                    flag[j] = 1
                    # print(f"Delete SQL {j} in Pattern {pattern}")
                    unique_num -= 1
                # print(f"Pattern {pattern} SQL {i} VS SQL {j} Similarity: {similarity}")
        print(f"{pattern}:\t{unique_num}")
        total_unique_num += unique_num
    print(f"Total Number:\t{total_num}")
    print(f"Total Unique Number:\t{total_unique_num}")