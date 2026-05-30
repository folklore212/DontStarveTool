FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY src/backend/general-web-backend/ .
RUN ./mvnw package -DskipTests -pl :steam-cache-service -am -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/steam-cache-service/target/*.jar /app/app.jar
USER app
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=40s \
  CMD curl -sf http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
