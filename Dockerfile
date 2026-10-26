# Multi-stage Dockerfile for Spring Boot Application

# Stage 1: Build the application using Maven with Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl

# Copy fat jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Configure execution environment
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
