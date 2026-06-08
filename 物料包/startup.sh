#!/bin/bash
# =============================================
#  知识服务挡板 — 启动脚本
#  用法: chmod +x startup.sh && ./startup.sh
# =============================================

# ==================== 配置区域（按需修改） ====================
SERVER_PORT=18080                          # 服务端口
DATA_PATH="./offline-data"                 # 离线数据包路径
JAVA_OPTS="-Xms256m -Xmx512m"             # JVM 参数
# =============================================================

# 切到脚本所在目录
cd "$(dirname "$0")"

JAR_NAME="mock-knowledge-service.jar"
LOG_DIR="logs"

mkdir -p "$LOG_DIR"

# ---- 杀旧进程 ----
OLD_PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    echo "[INFO] 停止旧进程 PID=$OLD_PID"
    kill $OLD_PID
    sleep 2
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "[INFO] 强杀进程 PID=$OLD_PID"
        kill -9 $OLD_PID
    fi
fi

# ---- 检查 jar 是否存在 ----
if [ ! -f "$JAR_NAME" ]; then
    echo "[ERROR] 找不到 $JAR_NAME，请确认脚本和 jar 在同一目录"
    exit 1
fi

# ---- 启动 ----
echo "[INFO] 启动 $JAR_NAME"
echo "[INFO]   端口: $SERVER_PORT"
echo "[INFO]   数据: $DATA_PATH"
echo "[INFO]   JVM:  $JAVA_OPTS"

nohup java $JAVA_OPTS -jar "$JAR_NAME" \
    --server.port="$SERVER_PORT" \
    --mock.data-path="$DATA_PATH" \
    > "$LOG_DIR/console.log" 2>&1 &

NEW_PID=$!

# ---- 等待确认 ----
sleep 3
if ps -p $NEW_PID > /dev/null 2>&1; then
    echo "[INFO] 启动成功 PID=$NEW_PID"
    echo "[INFO] 业务日志: $LOG_DIR/mock-knowledge-service.log"
    tail -5 "$LOG_DIR/mock-knowledge-service.log" 2>/dev/null \
        || echo "[WARN] 业务日志尚未生成，查看 console 日志: tail -f $LOG_DIR/console.log"
else
    echo "[ERROR] 启动失败，查看日志:"
    tail -20 "$LOG_DIR/console.log"
    exit 1
fi
