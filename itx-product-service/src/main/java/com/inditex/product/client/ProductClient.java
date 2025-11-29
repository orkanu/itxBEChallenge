package com.inditex.product.client;

import com.inditex.product.client.model.SimuladoProductDetails;

import java.util.List;

public interface ProductClient {
    SimuladoProductDetails getProductById(String productId);
    List<String> getSimilarProductIds(String productId);
}
