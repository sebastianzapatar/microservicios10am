# syntax=docker/dockerfile:1

# --- Etapa 1: compilación con Gradle -------------------------------------
FROM gradle:9.7.1-jdk25 AS build
WORKDIR /workspace

# Primero solo los scripts de build: esta capa se reutiliza mientras no
# cambien las dependencias, aunque cambie el código fuente.
COPY settings.gradle build.gradle ./
COPY eureka-server/build.gradle eureka-server/
COPY api-gateway/build.gradle api-gateway/
COPY doctor-service/build.gradle doctor-service/
COPY clinical-history-service/build.gradle clinical-history-service/
RUN --mount=type=cache,target=/home/gradle/.gradle,id=gradle-doctor-service \
    gradle :doctor-service:dependencies --configuration runtimeClasspath --no-daemon -q

COPY doctor-service/src doctor-service/src
RUN --mount=type=cache,target=/home/gradle/.gradle,id=gradle-doctor-service \
    gradle :doctor-service:bootJar --no-daemon

# --- Etapa 2: imagen de ejecución ----------------------------------------
FROM eclipse-temurin:25-jre AS runtime
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home --uid 10001 spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/doctor-service/build/libs/app.jar app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
