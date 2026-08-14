# syntax=docker/dockerfile:1.7

# Builder Pattern: this stage changes only when the Gradle build definition changes.
FROM eclipse-temurin:21-jdk-jammy AS dependencies
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --no-daemon dependencies

# Compile and test with the cached Gradle dependencies from the previous stage.
FROM dependencies AS builder
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon test bootJar

# Small, non-root production image. It contains neither source code nor Gradle.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
