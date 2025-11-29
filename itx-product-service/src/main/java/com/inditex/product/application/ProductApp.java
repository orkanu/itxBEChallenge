package com.inditex.product.application;

import com.inditex.product.application.model.ProductDetails;

import java.util.List;

public interface ProductApp {
    List<ProductDetails> getSimilarProducts(String productId);
}
