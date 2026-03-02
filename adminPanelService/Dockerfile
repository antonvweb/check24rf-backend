# Multi-stage build
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY ../common /build/common
COPY pom.xml /build/
COPY adminPanelService /build/adminPanelService
RUN mvn clean package -f /build/pom.xml -pl adminPanelService -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="check24rf"
LABEL version="1.0"
LABEL description="Admin Panel Service for Check24RF"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/adminPanelService/target/adminPanelService.jar /app/adminPanelService.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 16542

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:16542/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms128m", "-Xmx256m", "-jar", "/app/adminPanelService.jar", "--spring.profiles.active=docker"]
