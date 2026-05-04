FROM eclipse-temurin:21-jdk-alpine AS build
COPY . /src
WORKDIR /src
RUN ./mvnw -B package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/target/*.jar /app/app.jar
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
