FROM eclipse-temurin:25.0.4_7-jre
WORKDIR /app
COPY ./build/libs/specmatic-kafka-avro-sample.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
