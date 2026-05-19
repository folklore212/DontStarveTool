FROM eclipse-temurin:21-jre-alpine
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
RUN apk add --no-cache curl
RUN addgroup -S app && adduser -S app -G app
USER app
COPY app.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
