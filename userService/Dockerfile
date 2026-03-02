# Multi-stage build
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY ../common /build/common
COPY pom.xml /build/
COPY userService /build/userService
RUN mvn clean package -f /build/pom.xml -pl userService -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="check24rf"
LABEL version="1.0"
LABEL description="User Service for Check24RF"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/userService/target/userService.jar /app/userService.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 19384

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:19384/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms128m", "-Xmx256m", "-jar", "/app/userService.jar", "--spring.profiles.active=docker"]
