# itx-product-service

Backend service to find similar products for a given product ID

## Architecture

The service is divided into 3 layers:

- Application layer: contains the business logic and uses the client layer to interact with an external product service (the provided mock server)
- Client layer: contains the code to interact with the external product service (the provided mock server)
- Service layer: contains the code to expose the application layer as a REST API

## Running the service

To run the service, follow these steps:

1. Clone the repository
2. Run `./gradlew bootRun` to start the service
3. The service will be available at http://localhost:5000
4. To stop the service, press Ctrl+C

