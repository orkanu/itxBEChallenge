package com.inditex.product.application.ports.output;

import com.inditex.product.domain.model.ProductDetails;

import java.util.List;

// To be used by the client
public interface SimilarProducts {
    ProductDetails getProductById(String productId);
    List<String> getSimilarProductIds(String productId);
}
