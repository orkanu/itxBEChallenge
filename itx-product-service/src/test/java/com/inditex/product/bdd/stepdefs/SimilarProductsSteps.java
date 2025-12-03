package com.inditex.product.bdd.stepdefs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inditex.product.bdd.CucumberSpringConfig;
import com.inditex.product.infrastructure.adapters.in.dto.ProductDetailsDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SimilarProductsSteps extends CucumberSpringConfig {

    private final RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    private WireMockServer wireMockServer;
    private ResponseEntity<ProductDetailsDTO[]> response;
    private HttpStatusCode lastStatus;
    private String lastErrorBody;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void startWireMock() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(WireMockConfiguration.options().port(3001));
            wireMockServer.start();
            WireMock.configureFor("localhost", 3001);
        } else if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
        wireMockServer.resetAll();
    }

    @After
    public void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Given("there are the following products")
    public void thereAreTheFollowingProducts(DataTable dataTable) throws Exception {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        // For each product, return product details
        for (Map<String, String> row : rows) {
            String id = row.get("id").trim();
            String name = row.get("name").trim();
            Double price = parseDouble(row.get("price").trim());
            boolean availability = parseBoolean(row.get("availability").trim());

            Map<String, Object> body = new HashMap<>();
            body.put("id", id);
            body.put("name", name);
            body.put("price", price);
            body.put("availability", availability);

            wireMockServer.stubFor(get(urlEqualTo("/product/" + id))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(objectMapper.writeValueAsString(body))
                            .withStatus(200)));
        }
    }

    @And("products the following similar products")
    public void productsTheFollowingSimilarProducts(DataTable dataTable) throws Exception {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        // For each product ID, return similar product IDs
        for (Map<String, String> r : rows) {
            String id = r.get("id").trim();
            List<String> others = asList(r.get("similar").trim().split(","));
            wireMockServer.stubFor(get(urlEqualTo("/product/" + id + "/similarids"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(objectMapper.writeValueAsString(others))
                            .withStatus(200)));
        }
    }

    @When("I search by product ID {string}")
    public void i_search_by_product_id(String productId) {
        String url = "http://localhost:" + port + "/product/" + productId + "/similar";
        try {
            this.response = restTemplate.getForEntity(url, ProductDetailsDTO[].class);
            this.lastStatus = response.getStatusCode();
            this.lastErrorBody = null;
        } catch (HttpStatusCodeException e) {
            this.response = null;
            this.lastStatus = e.getStatusCode();
            this.lastErrorBody = e.getResponseBodyAsString();
        }
    }

    @Then("the response status is {string}")
    public void theResponseStatusIs(String status) {
        HttpStatus expected = HttpStatus.valueOf(status);
        if (response != null) {
            assertThat(response.getStatusCode()).isEqualTo(expected);
        }
        // Used to validate the HTTP status code when there's an error
        assertThat(lastStatus.value()).isEqualTo(expected.value());
        switch (expected) {
            case BAD_REQUEST:
                assertThat(lastStatus.is4xxClientError()).isTrue();
                break;
            case SERVICE_UNAVAILABLE:
                assertThat(lastStatus.is5xxServerError()).isTrue();
                break;
            default:
                break;
        }
    }

    @Then("the result contains")
    public void the_result_contains(DataTable dataTable) {
        assertThat(response).isNotNull();
        ProductDetailsDTO[] body = response.getBody();
        assertThat(body).isNotNull();

        List<Map<String, String>> expected = dataTable.asMaps(String.class, String.class);

        List<Map<String, String>> actual = Arrays.stream(body)
                .map(dto -> Map.of(
                        "id", dto.getId(),
                        "name", dto.getName(),
                        "price", format(java.util.Locale.US, "%.2f", dto.getPrice()),
                        "availability", Boolean.toString(dto.isAvailability())
                ))
                .collect(Collectors.toList());

        List<Map<String, String>> normalizedExpected = expected.stream().map(m -> {
            Map<String, String> n = new LinkedHashMap<>(m);
            n.put("price", format(java.util.Locale.US, "%.2f", parseDouble(m.get("price").trim())));
            n.put("availability", m.get("availability").trim());
            n.put("id", m.get("id").trim());
            n.put("name", m.get("name").trim());
            return n;
        }).collect(Collectors.toList());

        assertThat(actual).containsExactlyElementsOf(normalizedExpected);
    }

    @And("the result has {string} products")
    public void theResultHasProducts(String numProducts) {
        ProductDetailsDTO[] body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.length).isEqualTo(parseInt(numProducts));
    }

    @And("product ID {string} returns {string}")
    public void productIdReturns(String productId, String statusText) {
        String enumName = statusText.trim().toUpperCase().replace(' ', '_');
        HttpStatus status = HttpStatus.valueOf(enumName);
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId))
                .willReturn(aResponse()
                        .withStatus(status.value())));
    }

    @Then("the result contains error {string}")
    public void theResultContainsError(String expectedError) {
        assertThat(lastStatus).isNotNull();
        assertThat(lastErrorBody).isNotNull();
        assertThat(lastErrorBody).contains(expectedError);
    }
}
