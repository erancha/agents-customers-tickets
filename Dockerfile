FROM eclipse-temurin:21-jre

WORKDIR /app

# Runtime-only image: run `mvn clean package` outside Docker first so this copy step
# uses a fresh branch artifact (not stale/previous build output) and keeps build tools out.
ARG JAR_FILE=target/agents-customers-tickets-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
