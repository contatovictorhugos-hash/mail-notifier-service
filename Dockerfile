# Stage 1: Build da aplicação usando cache mount para dependências
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Stage 2: Imagem final leve
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/mailNotifierService-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
