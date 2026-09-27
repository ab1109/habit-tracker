# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dependencies first, so they're cached until pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Tests need Docker (Testcontainers), which isn't available inside a build;
# they run on your machine / in CI instead.
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/habit-tracker-*.jar app.jar
USER app

# Deployments sign in with Google by default. Override only for a private
# test box: SPRING_PROFILES_ACTIVE="" trusts the X-User-Id header (insecure).
ENV SPRING_PROFILES_ACTIVE=google
# Keep the heap within a small container's memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# The platform sets PORT; the app listens on it (default 8080).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
