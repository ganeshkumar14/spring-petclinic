# Use an official OpenJDK base image
FROM openjdk:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR file into the container.
# Replace 'your-application.jar' with the actual name of your built JAR file.
# Assuming your JAR is in the 'target/' directory after a Maven/Gradle build.
COPY target/spring-petclinic-3.3.0-SNAPSHOT.jar /app/spring-petclinic.jar

# Expose the port your application listens on (default for Spring Boot is 8080)
EXPOSE 8080

# Define the command to run your Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/spring-petclinic.jar"]