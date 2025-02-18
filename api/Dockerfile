FROM registry.access.redhat.com/ubi8/openjdk-17:1.18

WORKDIR /app

COPY . /app

RUN ./mvnw package

CMD ["java", "-Dquarkus.http.port=8080", "-jar", "target/quarkus-app/quarkus-run.jar"]
