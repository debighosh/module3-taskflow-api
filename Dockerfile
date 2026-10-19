# Stage 1: Build the JAR with Maven.
# Dependencies are resolved before the sources are copied so that layer is
# reused when only application code changes. Railway still builds this image
# from GitHub (target/ is not in git).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: Run the JAR in a slim runtime image.
# The process listens on 8080, which is the port Railway should route to.
FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=unversioned
LABEL org.opencontainers.image.title="taskflow-api" \
      org.opencontainers.image.version="${APP_VERSION}"
WORKDIR /app
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
COPY --from=build --chown=taskflow:taskflow /app/target/*.jar app.jar
USER taskflow
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
