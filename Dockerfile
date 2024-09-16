# Stage 1: Build the JAR file using Maven
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests

# Stage 2: Create the final image with the built JAR
FROM openjdk:17-jdk-slim

# Install curl to trigger healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/evaluation-service-0.0.1-SNAPSHOT.jar /app/evaluation.jar

ENTRYPOINT ["java", "-jar", "evaluation.jar"]
