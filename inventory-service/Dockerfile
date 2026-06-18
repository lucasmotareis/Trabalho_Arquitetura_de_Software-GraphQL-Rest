FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app/inventory-service

COPY inventory-service/pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY inventory-service/src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
