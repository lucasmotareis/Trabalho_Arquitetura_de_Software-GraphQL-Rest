FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app/order-service

COPY order-service/pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY order-service/src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/order-service/target/order-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
