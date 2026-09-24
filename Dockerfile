#FROM maven:3.9.9-eclipse-temurin-17 AS build
#WORKDIR /workspace
#COPY pom.xml ./
#RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
#COPY libs ./libs
#COPY src ./src
#COPY config ./config
#RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM registry.scc.spdbdev.com/baseimage/kylin10sp2amd64-openjdk17.0.18:1.0-2026M7

# 核心：基础镜像默认是非 root 用户，必须先显式切换为 root 才有权限在根目录下建目录
USER root

# 预先创建日志目录及工作目录，并赋予所有用户完全读写权限（777）
RUN mkdir -p /logs /app /app/logs && chmod -R 777 /logs /app

WORKDIR /app
COPY intelligent-qa-audit-service-*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

# 启动前自适应建目录并赋权，保障挂载卷场景下的写权限，通过 exec 保持 PID 1 优雅停机
ENTRYPOINT ["sh", "-c", "mkdir -p /logs /app/logs 2>/dev/null; chmod 777 /logs /app/logs 2>/dev/null || true; exec java -jar /app/app.jar"]
