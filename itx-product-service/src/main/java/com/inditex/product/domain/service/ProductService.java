package com.inditex.product.domain.service;

import com.inditex.product.application.ports.input.SimilarProductsUseCase;
import com.inditex.product.application.ports.output.SimilarProducts;
import com.inditex.product.domain.model.ProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.inditex.product.infrastructure.adapters.config.CacheConfig.CACHE;

@Service
public class ProductService implements SimilarProductsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final SimilarProducts similarProducts;

    @Autowired
    public ProductService(SimilarProducts similarProducts) {
        this.similarProducts = similarProducts;
    }

    @Override
    @Cacheable(value = CACHE, key = "#productId")
    public List<ProductDetails> getSimilarProducts(String productId) {
        List<String> similarProductIds = similarProducts.getSimilarProductIds(productId);

        if (similarProductIds == null || similarProductIds.isEmpty()) {
            logger.warn("Similar product IDs not found for product ID [{}]. Returning empty list", productId);
            return List.of();
        }

        return similarProductIds.stream()
                .map(similarProducts::getProductById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
