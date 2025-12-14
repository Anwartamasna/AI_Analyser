# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the locally built JAR from the target directory
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
