FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S app && adduser -S app -G app
COPY app.jar /app/app.jar
EXPOSE 8080
USER app
ENTRYPOINT exec java ${JAVA_OPTS:--XX:+UseG1GC -XX:MaxRAMPercentage=60} -jar /app/app.jar
