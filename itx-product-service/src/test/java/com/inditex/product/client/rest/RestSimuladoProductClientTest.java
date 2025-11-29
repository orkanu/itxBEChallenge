package com.inditex.product.client.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.inditex.product.client.model.SimuladoProductDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RestSimuladoProductClientTest {

    private WireMockServer wireMockServer;
    private RestSimuladoProductClient client;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(wireMockServer.baseUrl())
                .build();

        client = new RestSimuladoProductClient(restTemplate);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getProductById_shouldReturnMappedProduct() {
        String productId = "123";
        String body = """
                {
                  "id": "123",
                  "name": "Product 123",
                  "price": 19.99,
                  "availability": true
                }""";

        wireMockServer.stubFor(get("/product/" + productId)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        SimuladoProductDetails details = client.getProductById(productId);

        assertThat(details, notNullValue());
        assertThat(details.getId(), is("123"));
        assertThat(details.getName(), is("Product 123"));
        assertThat(details.getPrice(), is(19.99));
        assertThat(details.isAvailability(), is(true));
    }

    @Test
    void getSimilarProductIds_shouldReturnIdsFromArray() {
        String productId = "123";
        String body = """
                ["234","345","456"]
                """;

        wireMockServer.stubFor(get("/product/" + productId + "/similarids")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        List<String> ids = client.getSimilarProductIds(productId);

        assertThat(ids, contains("234", "345", "456"));
    }

    @Test
    void getSimilarProductIds_shouldReturnEmptyListOnNoContent() {
        String productId = "999";

        wireMockServer.stubFor(get("/product/" + productId + "/similarids")
                .willReturn(aResponse()
                        .withStatus(204)));

        List<String> ids = client.getSimilarProductIds(productId);

        assertThat(ids, is(empty()));
    }

    @Test
    void getProductById_shouldThrowClientExceptionOnNotFound() {
        String productId = "404";

        wireMockServer.stubFor(get("/product/" + productId)
                .willReturn(aResponse()
                        .withStatus(404)));

        try {
            client.getProductById(productId);
        } catch (com.inditex.product.client.exception.ClientException ex) {
            assertThat(ex.getMessage(), containsString("Product not found: " + productId));
            return;
        }
        throw new AssertionError("Expected ClientException to be thrown");
    }
}