# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /app
COPY backend ./backend
RUN --mount=type=cache,target=/root/.m2 mvn -q -f backend/pom.xml -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/backend/${MODULE}/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
