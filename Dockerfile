# ---------- Build stage ----------
# Use a Maven + JDK 21 image to compile and package the Spring Boot jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the build files first so Docker can cache the dependency download
# layer when only source code changes.
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline

# Now copy the source and build.
COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---------- Runtime stage ----------
# Smaller image for actually running the app — no Maven needed at runtime.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT; Spring Boot reads it via application.properties.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]