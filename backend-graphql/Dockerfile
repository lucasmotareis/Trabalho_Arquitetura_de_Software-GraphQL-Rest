FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app/backend-graphql

COPY backend-graphql/pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY backend-graphql/src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/backend-graphql/target/backend-graphql-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
