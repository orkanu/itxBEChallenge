package com.inditex.product.service;

import com.inditex.product.service.model.ProductDetailsDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductService {
    ResponseEntity<List<ProductDetailsDTO>> getSimilarProducts(String productId);
}
