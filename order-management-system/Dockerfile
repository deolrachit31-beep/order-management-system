FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /src
COPY pom.xml .
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ${SERVICE}/src ${SERVICE}/src
RUN mvn -B -pl ${SERVICE} -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG SERVICE
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /src/${SERVICE}/target/${SERVICE}.jar /app/app.jar
USER appuser
EXPOSE 8081 8082 8083 8084 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
