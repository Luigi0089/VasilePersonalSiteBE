# ====== STAGE 1: build ======
FROM maven:3.9.9-eclipse-temurin-21 AS build

# cartella di lavoro
WORKDIR /app

# copio il pom e i sorgenti
COPY pom.xml .
COPY src ./src

# build del jar (senza test)
RUN mvn clean package -DskipTests

# ====== STAGE 2: run ======
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# copio il jar generato dallo stage di build
COPY --from=build /app/target/*.jar app.jar

# Render passa la PORT come env var
ENV PORT=8080
EXPOSE 8080

# avvio dell'app Spring Boot
ENTRYPOINT ["java","-jar","app.jar"]
