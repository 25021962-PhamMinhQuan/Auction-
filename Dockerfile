FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/auction-server.jar app.jar
EXPOSE 2501
ENTRYPOINT ["java", "-jar", "app.jar"]