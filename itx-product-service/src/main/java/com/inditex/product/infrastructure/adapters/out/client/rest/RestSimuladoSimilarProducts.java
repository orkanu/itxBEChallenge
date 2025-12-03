package com.inditex.product.infrastructure.adapters.out.client.rest;

import com.inditex.product.application.ports.output.SimilarProducts;
import com.inditex.product.domain.exception.CircuitBreakerException;
import com.inditex.product.domain.exception.ProductNotFoundException;
import com.inditex.product.domain.model.ProductDetails;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    @CircuitBreaker(name = "client", fallbackMethod = "fallbackGetProductById")
    public ProductDetails getProductById(String productId) {
        String url = String.format("/product/%s" , productId);
        try {
            return restTemplate.getForObject(url, ProductDetails.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found: {}", productId);
            throw new ProductNotFoundException("Product not found: " + productId, e);
        }
    }

    private ProductDetails fallbackGetProductById(String productId, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            logger.warn("Product ID - circuit open", t);
            throw new CircuitBreakerException("Similar product service temporarily unavailable (circuit open)", t);
        }
        logger.warn("Product ID - error fetching", t);
        throw new CircuitBreakerException("There has been an error fetching product by ID", t);
    }

    @CircuitBreaker(name = "client", fallbackMethod = "fallbackGetSimilarProductIds")
    public List<String> getSimilarProductIds(String productId) {
        String url = String.format("/product/%s/similarids" , productId);

        String[] products = restTemplate.getForObject(url, String[].class);
        try {

            return (products == null) ? List.of() : Arrays.asList(products);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Similar product ids not found for product: " + productId, e);
        }
    }

    public List<String> fallbackGetSimilarProductIds(String productId, Throwable t) {
        logger.warn("Similar product IDs delayed or circuit open", t);
        if (t instanceof CallNotPermittedException) {
            throw new CircuitBreakerException("Similar product service temporarily unavailable (circuit open)", t);
        }
        throw new CircuitBreakerException("There has been an error fetching similar product ids", t);
    }
}
