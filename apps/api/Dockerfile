# Stage 1: compile the Spring Boot JAR (needs JDK + Maven).
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package && cp target/herdcommand-api-*.jar /src/app.jar

# Stage 2: run only the JAR (smaller JRE, no Maven).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /src/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
