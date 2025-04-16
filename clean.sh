#!/bin/bash

# 设置路径
Generate_Path="./sqlancer"
cases_path="./cases"
executor_path="./executor/__pycache__"
log_path="./log"

# 清理SQLancer的构建文件
echo "正在清理 SQLancer 构建文件..."
if [ -d "$Generate_Path/target" ]; then
  rm -rf "$Generate_Path/target"
  echo "清理 SQLancer 构建文件成功！"
else
  echo "SQLancer 构建文件不存在，跳过清理！"
fi

# 清理cases目录中的文件
echo "正在清理 cases 目录中的内容..."
if [ -d "$cases_path" ]; then
  rm -rf "$cases_path"/*
  echo "清理 cases 目录内容成功！"
else
  echo "cases 目录不存在，跳过清理！"
fi

# 清理log目录文件
echo "正在清理 log 目录中的内容..."
if [ -d "$log_path" ]; then
  rm -rf "$log_path"/*
  echo "清理 log 目录内容成功！"
else
  echo "log 目录不存在，跳过清理！"
fi

# 清理__pycache__目录中的文件
echo "正在清理 __pycache__ 目录中的内容..."
if [ -d "$executor_path" ]; then
  rm -rf "$executor_path"
  echo "清理 __pycache__ 目录内容成功！"
else
  echo "__pycache__ 目录不存在，跳过清理！"
fi

echo "清理完成！"