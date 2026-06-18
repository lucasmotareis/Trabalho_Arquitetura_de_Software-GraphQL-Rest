FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app/backend-rest

COPY backend-rest/pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY backend-rest/src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/backend-rest/target/backend-rest-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
