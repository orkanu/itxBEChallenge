package com.inditex.product.infrastructure.adapters.out.client.rest;

import com.inditex.product.application.ports.output.SimilarProducts;
import com.inditex.product.domain.exception.ProductNotFoundException;
import com.inditex.product.domain.model.ProductDetails;
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
public class RestSimuladoSimilarProducts implements SimilarProducts {

    private final Logger logger = LoggerFactory.getLogger(RestSimuladoSimilarProducts.class);

    private final RestTemplate restTemplate;

    @Autowired
    public RestSimuladoSimilarProducts(@Qualifier("simuladoProductClient") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ProductDetails getProductById(String productId) {
        String url = String.format("/product/%s" , productId);
        try {
            return restTemplate.getForObject(url, ProductDetails.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found: {}", productId);
            throw new ProductNotFoundException("Product not found: " + productId, e);
        }
    }

    public List<String> getSimilarProductIds(String productId) {
        String url = String.format("/product/%s/similarids" , productId);

        String[] products = restTemplate.getForObject(url, String[].class);
        try {

            return (products == null) ? List.of() : Arrays.asList(products);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Similar product ids not found for product: " + productId, e);
        }
    }
}
