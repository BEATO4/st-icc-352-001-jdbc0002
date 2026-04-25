FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/install/FINAL /app/

RUN chmod +x /app/bin/FINAL

EXPOSE 8080
EXPOSE 9090

ENTRYPOINT ["/app/bin/FINAL"]
