# FarmJM Web: imagen para Render (o cualquier host con Docker).
# Etapa 1: compilar con Maven. Etapa 2: ejecutar solo el JAR con una JRE liviana.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/farmjm-2.0.jar app.jar
# Los datos de la base llegan por variables de entorno (FARMJM_DB_URL, FARMJM_DB_USER, FARMJM_DB_PASSWORD).
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC -Djava.awt.headless=true"
EXPOSE 10000
CMD ["java", "-cp", "app.jar", "com.unev.farmjm.web.WebServer"]
