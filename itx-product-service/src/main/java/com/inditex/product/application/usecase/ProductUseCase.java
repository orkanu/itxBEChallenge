package com.inditex.product.application.usecase;

import com.inditex.product.application.ProductApp;
import com.inditex.product.application.exception.ApplicationException;
import com.inditex.product.application.mapper.ProductDetailsMapper;
import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.client.ProductClient;
import com.inditex.product.client.exception.ClientException;
import com.inditex.product.client.model.SimuladoProductDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.inditex.product.application.mapper.ProductDetailsMapper.MAPPER;

@Service
public class ProductUseCase implements ProductApp {

    private final ProductClient simuladoProductClient;

    @Autowired
    public ProductUseCase(ProductClient simuladoProductClient) {
        this.simuladoProductClient = simuladoProductClient;
    }

    @Cacheable(
            value = "similarProductsCache",
            key = "#productId")
    public List<ProductDetails> getSimilarProducts(String productId) {
        List<String> similarProductIds = simuladoProductClient.getSimilarProductIds(productId);
        if (similarProductIds == null || similarProductIds.isEmpty()) {
            return List.of();
        }

        try {

            return MAPPER.toProductDetails(similarProductIds.stream()
                    .map(simuladoProductClient::getProductById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        } catch (ClientException e) {
            throw new ApplicationException(e.getMessage());
        }
    }
}
