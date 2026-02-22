FROM eclipse-temurin:17-jre
WORKDIR /app
COPY ./build/libs/specmatic-kafka-avro-sample.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
