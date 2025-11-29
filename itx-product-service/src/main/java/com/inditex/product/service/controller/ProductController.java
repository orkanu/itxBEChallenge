package com.inditex.product.service.controller;

import com.inditex.product.application.ProductApp;
import com.inditex.product.service.ProductService;
import com.inditex.product.service.model.ProductDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.inditex.product.service.mapper.ProductDTOMapper.MAPPER;

@RestController
@RequestMapping("/product")
public class ProductController extends ProductControllerBase implements ProductService {
    private final ProductApp productUseCase;

    @Autowired
    public ProductController(ProductApp productUseCase) {
        this.productUseCase = productUseCase;
    }

    @GetMapping(value = "/{productId}/similar", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ProductDetailsDTO>> getSimilarProducts(@PathVariable("productId") String productId) {
        validate(productId);
        return ResponseEntity.ok(MAPPER.toProductDetailsDTO(productUseCase.getSimilarProducts(productId)));
    }

    // For simplicity, I leave this validation in the controller. For a bigger service with more request mappings where
    // the same validation needs to be applied to different requests, I would move it to a common validation class
    private void validate(String productId) throws IllegalArgumentException {
        try{
            Integer.parseInt(productId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid productId");
        }
    }
}
