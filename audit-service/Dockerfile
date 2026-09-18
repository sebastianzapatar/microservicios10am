# syntax=docker/dockerfile:1

FROM gradle:9.7.1-jdk25 AS build
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY eureka-server/build.gradle eureka-server/
COPY api-gateway/build.gradle api-gateway/
COPY doctor-service/build.gradle doctor-service/
COPY clinical-history-service/build.gradle clinical-history-service/
COPY audit-service/build.gradle audit-service/
RUN --mount=type=cache,target=/home/gradle/.gradle,id=gradle-audit-service \
    gradle :audit-service:dependencies --configuration runtimeClasspath --no-daemon -q

COPY audit-service/src audit-service/src
RUN --mount=type=cache,target=/home/gradle/.gradle,id=gradle-audit-service \
    gradle :audit-service:bootJar --no-daemon

FROM eclipse-temurin:25-jre AS runtime
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home --uid 10001 spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/audit-service/build/libs/app.jar app.jar
USER spring
EXPOSE 8084
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
