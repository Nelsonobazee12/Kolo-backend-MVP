# Build stage
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN gradle bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S kolo && adduser -S kolo -G kolo

COPY --from=build /app/build/libs/*.jar app.jar

# Own the jar file
RUN chown kolo:kolo app.jar

USER kolo

EXPOSE 8080

# JVM tuning for container environment
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]