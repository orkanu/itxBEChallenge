package com.inditex.product.application.usecase;

import com.inditex.product.application.ProductApp;
import com.inditex.product.application.exception.ApplicationException;
import com.inditex.product.application.exception.BreakerApplicationException;
import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.client.ProductClient;
import com.inditex.product.client.exception.ClientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.inditex.product.application.config.CacheConfig.CACHE;
import static com.inditex.product.application.mapper.ProductDetailsMapper.MAPPER;

@Service
public class ProductUseCase implements ProductApp {

    private final Logger logger = LoggerFactory.getLogger(ProductUseCase.class);

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final ProductClient simuladoProductClient;

    @Autowired
    public ProductUseCase(CircuitBreakerFactory<?, ?> circuitBreakerFactory, ProductClient simuladoProductClient) {
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.simuladoProductClient = simuladoProductClient;
    }

    @Cacheable(
            value = CACHE,
            key = "#productId")
    public List<ProductDetails> getSimilarProducts(String productId) {
        CircuitBreaker cb = circuitBreakerFactory.create("productClient");
        List<String> similarProductIds = cb.run(() ->
                        simuladoProductClient.getSimilarProductIds(productId),
                throwable -> {
                    logger.warn("Similar product IDs delayed or circuit open", throwable);
                    if (throwable instanceof CallNotPermittedException) {
                        throw new BreakerApplicationException("Similar product service temporarily unavailable (circuit open)", throwable);
                    } else {
                        throw new BreakerApplicationException("There has been an error fetching similar product ids", throwable);
                    }
                });

        if (similarProductIds == null || similarProductIds.isEmpty()) {
            logger.warn("Similar product IDs not found for product ID [{}]. Returning empty list", productId);
            return List.of();
        }

        try {
            return MAPPER.toProductDetails(similarProductIds.stream()
                    .map(p -> cb.run(() ->
                                    simuladoProductClient.getProductById(p),
                            t -> {
                                if (t instanceof CallNotPermittedException) {
                                    logger.warn("Product ID - circuit open", t);
                                    throw new BreakerApplicationException("Similar product service temporarily unavailable (circuit open)", t);
                                } else {
                                    logger.warn("Product ID - error fetching", t);
                                    throw new BreakerApplicationException("There has been an error fetching product by ID", t);
                                }
                            }))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        } catch (ClientException e) {
            logger.error("Error getting similar products for product ID [{}]: {}", productId, e.getMessage());
            throw new ApplicationException(e.getMessage());
        }
    }
}
