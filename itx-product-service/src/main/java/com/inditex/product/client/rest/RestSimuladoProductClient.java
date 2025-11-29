package com.inditex.product.client.rest;

import com.inditex.product.client.ProductClient;
import com.inditex.product.client.exception.ClientException;
import com.inditex.product.client.model.SimuladoProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Component
public class RestSimuladoProductClient implements ProductClient {

    private final Logger logger = LoggerFactory.getLogger(RestSimuladoProductClient.class);

    private final RestTemplate restTemplate;

    @Autowired
    public RestSimuladoProductClient(@Qualifier("simuladoProductClient") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SimuladoProductDetails getProductById(String productId) {
        String url = String.format("/product/%s" , productId);
        try {
            return restTemplate.getForObject(url, SimuladoProductDetails.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found: {}", productId);
            throw new ClientException("Product not found: " + productId, e);
        } catch (HttpClientErrorException e) {
            logger.warn("Client error calling getProductById for id [{}]: {}", productId, e.getMessage());
            throw new ClientException("Client error calling getProductById for: " + productId, e);
        }
    }

    public List<String> getSimilarProductIds(String productId) {
        String url = String.format("/product/%s/similarids" , productId);

        String[] products = restTemplate.getForObject(url, String[].class);
        try {

            return (products == null) ? List.of() : Arrays.asList(products);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("Similar product ids not found for product: " + productId, e);
        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error calling getSimilarProductIds for: " + productId, e);
        }
    }
}
