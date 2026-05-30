FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY src/backend/general-web-backend/ .
RUN ./mvnw package -DskipTests -pl :server-service -am -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/server-service/target/*.jar /app/app.jar
USER app
EXPOSE 8083
HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=60s \
  CMD curl -sf http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
