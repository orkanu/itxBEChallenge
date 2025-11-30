FROM amazoncorretto:21.0.9-alpine3.22

WORKDIR /app

COPY target/itx-product-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "app.jar"]