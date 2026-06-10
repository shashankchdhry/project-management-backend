# syntax=docker/dockerfile:1

# ----- Build stage -----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache dependencies in their own layer: only re-run when pom/wrapper change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Build the application.
COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package

# ----- Runtime stage -----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]