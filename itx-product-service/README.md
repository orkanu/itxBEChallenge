# itx-product-service

Backend service to find similar products for a given product ID

## Architecture

The service tries to use an hexagonal architecture:

- Domain: contains the business logic and uses the client adapter to interact with an external product service (the provided mock server)
- Adapters:
  - Controller: contains the code to expose the application layer as a REST API 
  - Client: contains the code to interact with the external product service (the provided mock server)

### Domain

Contains the business logic to find similar products.
It adheres to the contract defined in the `application.ports.input.SimilarProductsUseCase` interface.
Having a client injected, it first grabs similar product IDs and, once retrieved, it then retrieves the product details for each of them.
It has a cache configured to avoid calling the external service too often.

If the client throws an exception, the application will wrap it in an ApplicationException and rethrow it. The expectation is
to handle this exception in the service layer and return a 500 error code or a 404 error code depending on the situation.

### Client adapter

Contains a REST client to interact with the mocks server.
It adheres to the contract defined in the `application.ports.output.SimilarProducts` interface that way, if we need to use a different client
to retrieve the data from a different source (like via MQ or SOAP) it can be easily replaced.
It has a circuit breaker to avoid overloading the server (the provided mock in our case)

### Controller adapter

Exposes the application layer as a REST API. 
It handles the exceptions thrown in the domain and returns appropriate HTTP status codes.

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

## Testing

Running `mvn clean install` will run unit and BDD (Cucumber) tests.

Cucumber reports will be generated in `target/cucumber-html-reports` directory.

### Improvements

The following improvements could be made:

- Improve error handling being more specific about the cause of the error
- Use UUIDs when logging errors to help tracing
- Use correlation IDs to trace requests across services
- Move to non-blocking I/O to improve performance and reduce latency using WebClient instead of RestTemplate, for example
- Add more tests to increase edge case coverage
