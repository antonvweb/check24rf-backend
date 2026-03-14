# Multi-stage build
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml /build/
COPY common /build/common
COPY mcoService /build/mcoService
COPY userService /build/userService
COPY authService /build/authService
RUN mvn clean package -f /build/pom.xml -pl mcoService -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="check24rf"
LABEL version="1.0"
LABEL description="MCO Service for Check24RF"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/mcoService/target/mcoService.jar /app/mcoService.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 17456

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:17456/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms256m", "-Xmx512m", "-jar", "/app/mcoService.jar", "--spring.profiles.active=docker"]
