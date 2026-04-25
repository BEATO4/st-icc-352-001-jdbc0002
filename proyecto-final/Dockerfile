FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
# Compilamos de forma estándar
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
# Copiamos la distribución completa
COPY --from=builder /app/build/install/FINAL /app/

# Aseguramos permisos de ejecución para el script de arranque
RUN chmod +x /app/bin/FINAL

EXPOSE 8080
EXPOSE 9090

# Usamos el script oficial generado por Gradle
ENTRYPOINT ["/app/bin/FINAL"]
