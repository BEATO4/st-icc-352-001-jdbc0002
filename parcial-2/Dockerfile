# ============================================================
# Stage 1 — Build (Gradle + JDK 21)
# ============================================================
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy build files first for better layer caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached separately)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build fat JAR
RUN gradle clean shadowJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m"

# ============================================================
# Stage 2 — Runtime (slim JRE 21)
# ============================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the fat JAR from build stage
COPY --from=builder /app/build/libs/*-all.jar app.jar

# Expose HTTP port
EXPOSE 7070

# H2 TCP server data directory
VOLUME ["/app/data"]

# Environment defaults (override in docker-compose)
ENV APP_PORT=7070 \
    H2_PORT=9092 \
    DB_PATH=/app/data/eventos_db

ENTRYPOINT ["java", "-jar", "app.jar"]
