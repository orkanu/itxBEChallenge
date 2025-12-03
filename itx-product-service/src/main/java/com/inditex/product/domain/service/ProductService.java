package com.inditex.product.domain.service;

import com.inditex.product.application.ports.input.SimilarProductsUseCase;
import com.inditex.product.application.ports.output.SimilarProducts;
import com.inditex.product.domain.exception.ApplicationException;
import com.inditex.product.domain.exception.CircuitBreakerException;
import com.inditex.product.domain.exception.ProductNotFoundException;
import com.inditex.product.domain.model.ProductDetails;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.inditex.product.infrastructure.adapters.config.CacheConfig.CACHE;

@Service
public class ProductService implements SimilarProductsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final CircuitBreaker circuitBreaker;
    private final SimilarProducts similarProducts;

    @Autowired
    public ProductService(CircuitBreaker circuitBreaker,
                          SimilarProducts similarProducts) {
        this.circuitBreaker = circuitBreaker;
        this.similarProducts = similarProducts;
    }

    @Override
    @Cacheable(value = CACHE, key = "#productId")
    public List<ProductDetails> getSimilarProducts(String productId) {
        List<String> similarProductIds = circuitBreaker.run(() -> similarProducts.getSimilarProductIds(productId), throwable -> {
            logger.warn("Similar product IDs delayed or circuit open", throwable);
            if (throwable instanceof CallNotPermittedException) {
                throw new CircuitBreakerException("Similar product service temporarily unavailable (circuit open)", throwable);
            }
            throw new CircuitBreakerException("There has been an error fetching similar product ids", throwable);
        });

        if (similarProductIds == null || similarProductIds.isEmpty()) {
            logger.warn("Similar product IDs not found for product ID [{}]. Returning empty list", productId);
            return List.of();
        }

        try {
            return similarProductIds.stream()
                    .map(p -> circuitBreaker.run(() -> similarProducts.getProductById(p), t -> {
                        if (t instanceof CallNotPermittedException) {
                            logger.warn("Product ID - circuit open", t);
                            throw new CircuitBreakerException("Similar product service temporarily unavailable (circuit open)", t);
                        }
                        logger.warn("Product ID - error fetching", t);
                        throw new CircuitBreakerException("There has been an error fetching product by ID", t);
                    }))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (ProductNotFoundException e) {
            logger.error("Error getting similar products for product ID [{}]: {}", productId, e.getMessage());
            throw new ApplicationException(e.getMessage());
        }
    }
}
