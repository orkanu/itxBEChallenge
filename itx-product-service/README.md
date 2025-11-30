# itx-product-service

Backend service to find similar products for a given product ID

## Architecture

The service is divided into 3 layers:

- Application layer: contains the business logic and uses the client layer to interact with an external product service (the provided mock server)
- Client layer: contains the code to interact with the external product service (the provided mock server)
- Service layer: contains the code to expose the application layer as a REST API

### Client layer

Contains a REST client to interact with the mocks server. 
It adheres to the contract defined in the `ProductClient` interface that way, if we need to use a different client
to retrieve the data from a different source (like via MQ or SOAP) it can be easily replaced.

### Application layer

Contains the business logic to find similar products. 
Using the client it first grabs similar product IDs. Once retrieved, it then uses the client to retrieve the actual products details.

If the client throws an exception, the application will wrap it in an ApplicationException and rethrow it. The expectation is
to handle this exception in the service layer and return a 500 error code or a 404 error code depending on the situation.

### Service layer

Exposes the application layer as a REST API. It handles the exceptions thrown by the application layer and returns 
appropriate HTTP status codes.

## Running the service

### Via Maven

To run the service using Maven, follow these steps:

1. Clone the repository
2. Run `mvn spring-boot:run` to start the service
3. The service will be available at http://localhost:5000
4. To stop the service, press Ctrl+C

### Via Docker

To run the service using Docker, follow these steps:

1. Clone the repository
2. Inside `ìtx-product-service`, run `mvn package` to build the JAR file
3. From the root of the repository, where the `docker-compose.yaml` file is located, run `docker compose up -d simulado influxdb grafana itx-product-service` to get the service and the mock server running
4. The service will be available at http://localhost:5000
5. To stop everything, run `docker compose down`

