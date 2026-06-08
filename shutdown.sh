#!/bin/bash
# =============================================
#  知识服务挡板 — 停止脚本
#  用法: chmod +x shutdown.sh && ./shutdown.sh
# =============================================

JAR_NAME="mock-knowledge-service.jar"

PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "[INFO] 未找到运行中的 $JAR_NAME"
    exit 0
fi

echo "[INFO] 停止进程 PID=$PID"
kill $PID
sleep 2

if ps -p $PID > /dev/null 2>&1; then
    echo "[INFO] 强杀进程 PID=$PID"
    kill -9 $PID
    sleep 1
fi

if ps -p $PID > /dev/null 2>&1; then
    echo "[ERROR] 停止失败 PID=$PID"
    exit 1
else
    echo "[INFO] 已停止"
fi
