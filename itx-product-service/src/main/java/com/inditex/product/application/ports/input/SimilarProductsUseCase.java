package com.inditex.product.application.ports.input;

import com.inditex.product.domain.model.ProductDetails;

import java.util.List;

public interface SimilarProductsUseCase {
    List<ProductDetails> getSimilarProducts(String productId);
}
