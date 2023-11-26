FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/evaluation-service-0.0.1-SNAPSHOT.jar ./evaluation.jar

EXPOSE 8082

CMD ["java", "-jar", "evaluation.jar"]
