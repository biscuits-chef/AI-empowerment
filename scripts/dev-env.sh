#!/usr/bin/env bash
# ==============================================================================
# 本地开发环境环境变量设置与执行脚本 (兼容 Linux / macOS / Bash / Zsh)
#
# 使用方式:
#   1. 直接运行（自动切入带环境变量的 Shell 会话）：
#      ./scripts/dev-env.sh
#
#   2. source 加载（在当前终端生效）：
#      source ./scripts/dev-env.sh   或者   . ./scripts/dev-env.sh
#
#   3. 直接带命令启动（自动带上环境变量执行指定命令）：
#      ./scripts/dev-env.sh mvn spring-boot:run
# ==============================================================================

echo "⚙️  正在加载本地开发环境配置..."

# ------------------------------------------------------------------------------
# 1. 数据库配置 (MySQL / GoldenDB)
# ------------------------------------------------------------------------------
export DEV_DB_TYPE="mysql"
export DEV_MYSQL_URL="jdbc:mysql://127.0.0.1:3306/intelligent_qa?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false"
export DEV_MYSQL_USERNAME="root"
export DEV_MYSQL_PASSWORD="123456"
export DEV_DB_POOL_SIZE="10"
export DEV_DB_MIN_IDLE="2"

# ------------------------------------------------------------------------------
# 2. 表结构初始化与 Flyway 开关
# ------------------------------------------------------------------------------
export DEV_FLYWAY_ENABLED="false"
export DEV_SQL_INIT_MODE="always"

# ------------------------------------------------------------------------------
# 3. 认证与用户模拟
# ------------------------------------------------------------------------------
export DEV_AUTH_MODE="mock"
export DEV_MOCK_USER_ID="dev-user-001"

# ------------------------------------------------------------------------------
# 4. 问答与业务查询
# ------------------------------------------------------------------------------
export DEV_QA_DEMO_MODE="true"
export DEV_BUSINESS_QUERY_ENABLED="true"

# ------------------------------------------------------------------------------
# 5. Redis 缓存配置 (默认未启用)
# ------------------------------------------------------------------------------
export DEV_REDIS_ENABLED="false"
export DEV_REDIS_HOST="127.0.0.1"
export DEV_REDIS_PORT="6379"
export DEV_REDIS_PASSWORD=""

# ------------------------------------------------------------------------------
# 6. 大模型 (HiAgent / 公司模型) 配置
# ------------------------------------------------------------------------------
export DEV_COMPANY_MODEL_ENABLED="false"
export DEV_COMPANY_MODEL_BASE_URL="http://127.0.0.1:6789"
export DEV_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED="false"

# ------------------------------------------------------------------------------
# 7. 本地 Ollama 向量模型配置
# ------------------------------------------------------------------------------
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="nomic-embed-text"

# ------------------------------------------------------------------------------
# 8. 对象存储与文件模式
# ------------------------------------------------------------------------------
export DEV_OBS_ENABLED="false"
export DEV_FILE_STORAGE_MODE="local"
export DEV_FILE_ALLOW_UNPROCESSED_READY="true"

# ------------------------------------------------------------------------------
# 自动生效与执行模式处理
# ------------------------------------------------------------------------------

# 模式 1：如果后面跟了命令（例如 ./scripts/dev-env.sh mvn spring-boot:run），直接带环境变量运行
if [ $# -gt 0 ]; then
    echo "🚀 正在使用已配置的环境变量执行命令: $*"
    exec "$@"
fi

# 检测是否是通过 source / . 加载的
is_sourced=0
if [ -n "$ZSH_VERSION" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) is_sourced=1;; esac
elif [ -n "$BASH_VERSION" ]; then
    [ "${BASH_SOURCE[0]}" != "$0" ] && is_sourced=1
fi

# 模式 2：如果是直接敲 ./scripts/dev-env.sh 执行的，自动拉起包含这些环境变量的新 Shell
if [ "$is_sourced" -eq 0 ]; then
    echo "💡 检测到你是直接执行脚本，已自动为你切入带有上述环境变量的 Shell 会话！"
    echo "👉 环境变量已全面就绪，你可以在当前终端直接运行启动命令了。"
    exec "${SHELL:-/bin/bash}"
else
    echo "✅ 环境变量已成功注入当前 Shell 会话！"
fi
