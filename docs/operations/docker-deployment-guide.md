# Docker 容器化打包与部署命令速查手册

本文档整理了本项目（后端 `AI-empowerment` 与前端 `AI-empowerment-web`）在构建、离线导出、FTP 传输、上传镜像仓库以及多环境（Dev/Test/Prod）部署时所需的完整命令。

---

## 目录
1. [后端镜像构建（docker build）](#1-后端镜像构建docker-build)
2. [镜像离线导出与导入（FTP 传输流程）](#2-镜像离线导出与导入ftp-传输流程)
3. [镜像打标签与上传仓库（docker push）](#3-镜像打标签与上传仓库docker-push)
4. [多环境运行命令（docker run）](#4-多环境运行命令docker-run)
5. [容器日常运维与排障命令](#5-容器日常运维与排障命令)
6. [前端镜像构建与部署（AI-empowerment-web）](#6-前端镜像构建与部署ai-empowerment-web)

---

## 1. 后端镜像构建（docker build）

> **工作目录**：请确保在 `AI-empowerment` 后端工程根目录下执行。

### 1.1 本地开发/Mac 本机运行构建（ARM64 架构）
直接使用项目内的多阶段 `Dockerfile`，容器内全自动编译并生成镜像：
```bash
docker build -t intelligent-qa-service:latest .
```

### 1.2 针对公司 Linux 生产/测试服务器构建（强制 x86 / AMD64）
> **重要**：如果您的开发机是 Apple Silicon Mac（M 系列芯片），打出给 x86 生产服务器运行的镜像**必须**加上 `--platform linux/amd64`，防止启动报 `exec format error`。

```bash
docker build --platform linux/amd64 -t intelligent-qa-service:v1.0.0 .
```

### 1.3 如果怀疑缓存影响，强制全量重编构建
```bash
docker build --no-cache --platform linux/amd64 -t intelligent-qa-service:v1.0.0 .
```

### 1.4 双架构通用镜像构建（同时支持 ARM 鲲鹏与 x86 服务器）
```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t harbor.example.com/ai/intelligent-qa-service:v1.0.0 \
  --push .
```

---

## 2. 镜像离线导出与导入（FTP 传输流程）

适用于开发机与生产服务器网络隔离、需通过 FTP / 跳板机传输的场景。

### 2.1 【本地 Mac】将打好的镜像导出为 `.tar` 离线文件
```bash
# 语法：docker save -o <文件名.tar> <镜像名:版本号> 替换为自己的路径
docker save -o /Users/biscuits/Public/work/intelligent-qa-service-v1.0.0.tar intelligent-qa-service:v1.0.0
```
> 执行后会在当前目录生成 `intelligent-qa-service-v1.0.0.tar` 文件（体积约 200MB~300MB）。

### 2.2 【通过 FTP】上传 `.tar` 文件到目标服务器
使用 FileZilla、Xftp 或命令行将 `intelligent-qa-service-v1.0.0.tar` 上传到服务器指定目录（如 `/opt/packages/`）。

### 2.3 【服务器端】将 `.tar` 离线包导入 Docker 引擎
登录服务器，进入对应目录执行：
```bash
docker load -i intelligent-qa-service-v1.0.0.tar
```
导入完成后，运行 `docker images` 即可看到镜像已载入。

---

## 3. 镜像打标签与上传仓库（docker push）

### 3.1 登录私有镜像仓库（Harbor / 云仓库）
```bash
docker login harbor.example.com
# 按照提示输入用户名和密码/访问凭证
```

### 3.2 给镜像打上目标仓库规范的标签（Tag）
```bash
# 语法：docker tag <本地镜像:标签> <仓库地址>/<命名空间>/<服务名>:<版本>
docker tag intelligent-qa-service:v1.0.0 harbor.example.com/ai/intelligent-qa-service:v1.0.0
```

### 3.3 推送镜像到仓库
```bash
docker push harbor.example.com/ai/intelligent-qa-service:v1.0.0
```

---

## 4. 多环境运行命令（docker run）

> **核心原则**：镜像只打包一次，通过 `-e SPRING_PROFILES_ACTIVE` 切换激活的环境配置（读取对应的 `application-dev.xml` / `application-test.xml` / `application-prod.xml`）。

### 4.1 开发环境（Dev）启动
```bash
docker run -d \
  --name intelligent-qa-dev \
  -p 8080:8080 \
  -v $(pwd)/logs:/app/logs \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DEV_DB_URL="jdbc:mysql://10.x.x.x:3306/intelligent_qa?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" \
  -e DEV_DB_USERNAME="intelligent_qa" \
  -e DEV_DB_PASSWORD="dev_password" \
  -e DEV_REDIS_HOST="10.x.x.x" \
  -e DEV_REDIS_PORT="6379" \
  -e DEV_QA_DEMO_MODE="false" \
  intelligent-qa-service:latest
```

### 4.2 测试环境（Test）启动
```bash
docker run -d \
  --name intelligent-qa-test \
  -p 8080:8080 \
  -v /data/logs/intelligent-qa:/app/logs \
  -e SPRING_PROFILES_ACTIVE=test \
  -e TEST_DB_URL="jdbc:mysql://test-mysql:3306/intelligent_qa?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" \
  -e TEST_DB_USERNAME="test_user" \
  -e TEST_DB_PASSWORD="test_password" \
  -e TEST_REDIS_HOST="test-redis" \
  -e TEST_REDIS_PORT="6379" \
  harbor.example.com/ai/intelligent-qa-service:v1.0.0
```

### 4.3 生产环境（Prod）启动
```bash
docker run -d \
  --name intelligent-qa-prod \
  -p 8080:8080 \
  --restart always \
  -v /data/logs/intelligent-qa:/app/logs \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PROD_DB_URL="jdbc:mysql://prod-goldendb:3306/intelligent_qa?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" \
  -e PROD_DB_USERNAME="prod_user" \
  -e PROD_DB_PASSWORD="prod_password" \
  -e PROD_REDIS_HOST="prod-redis" \
  -e PROD_REDIS_PORT="6379" \
  harbor.example.com/ai/intelligent-qa-service:v1.0.0
```

---

## 5. 容器日常运维与排障命令

### 5.1 查看容器状态与健康检查情况
```bash
# 查看正在运行的容器及健康状态（Up xx minutes (healthy)）
docker ps

# 查看具体健康检查探针执行历史
docker inspect --format='{{json .State.Health}}' intelligent-qa-prod | jq .
```

### 5.2 查看运行日志
```bash
# 1. 实时跟踪容器控制台标准输出（最后 200 行）
docker logs -f --tail 200 intelligent-qa-prod

# 2. 查看挂载到宿主机的日志文件
tail -f /data/logs/intelligent-qa/intelligent-qa-audit-service.log
tail -f /data/logs/intelligent-qa/intelligent-qa-audit-service-error.log
```

### 5.3 手动验证健康检查接口
```bash
# 验证就绪探针（返回 {"status":"UP"} 代表启动正常就绪）
curl http://localhost:8080/actuator/health/readiness

# 验证存活探针
curl http://localhost:8080/actuator/health/liveness
```

### 5.4 进入容器内部终端排查
```bash
docker exec -it intelligent-qa-prod sh
```

### 5.5 停止与删除容器
```bash
# 优雅停机（向 Spring Boot 发送 SIGTERM 信号）
docker stop intelligent-qa-prod

# 删除旧容器（不影响镜像）
docker rm intelligent-qa-prod
```

---

## 6. 前端镜像构建与部署（AI-empowerment-web）

> **工作目录**：请确保在 `AI-empowerment-web` 前端工程根目录下执行。

### 6.1 前端镜像构建
```bash
docker build -t intelligent-qa-web:v1.0.0 .
```

### 6.2 前端容器运行（带 Nginx 反向代理配置）
```bash
docker run -d \
  --name intelligent-qa-web \
  -p 80:8080 \
  --restart always \
  -e BACKEND_UPSTREAM="http://<后端服务器内网IP>:8080" \
  -e APP_PROFILE=prod \
  intelligent-qa-web:v1.0.0
```
> 前端启动后，通过浏览器访问 `http://<服务器IP>/` 即可使用，Nginx 会自动将 `/api/` 路由转发给指定的后端地址。
