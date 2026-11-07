# Etapa 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/comaticket-0.0.1-SNAPSHOT.jar app.jar

# Crear carpeta de montaje local por si no hay EFS externo montado
RUN mkdir -p ./efs-data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


