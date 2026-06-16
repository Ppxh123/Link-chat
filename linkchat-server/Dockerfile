# ===================================
# LinkChat Server Dockerfile
# 阶段1: Maven构建
# 阶段2: JRE运行
# ===================================

FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非root用户
RUN addgroup -S linkchat && adduser -S linkchat -G linkchat

# 复制JAR
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R linkchat:linkchat /app

USER linkchat

EXPOSE 8080

# JVM优化参数
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]