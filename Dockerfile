#FROM maven:3.9.9-eclipse-temurin-17 AS build
#WORKDIR /workspace
#COPY pom.xml ./
#RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
#COPY libs ./libs
#COPY src ./src
#COPY config ./config
#RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM registry.scc.spdbdev.com/baseimage/kylin10sp2amd64-openjdk17.0.18:1.0-2026M7
#RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
#COPY --from=build /workspace/target/intelligent-qa-audit-service-*.jar app.jar
COPY intelligent-qa-audit-service-*.jar app.jar
#USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
