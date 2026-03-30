FROM gradle:8.8-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon clean installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/install/practica-5 /app
EXPOSE 7000
ENV DB_VENDOR=postgres
ENV DB_URL=jdbc:postgresql://db:5432/blogdb
ENV DB_USER=blog
ENV DB_PASSWORD=blog
ENV HIBERNATE_DDL_AUTO=update
CMD ["/app/bin/practica-5"]

