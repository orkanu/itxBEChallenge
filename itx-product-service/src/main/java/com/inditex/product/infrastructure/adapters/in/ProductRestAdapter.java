package com.inditex.product.infrastructure.adapters.in;

import com.inditex.product.application.ports.input.SimilarProductsUseCase;
import com.inditex.product.infrastructure.adapters.in.dto.ProductDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.inditex.product.infrastructure.adapters.in.mapper.ProductDTOMapper.MAPPER;

@RestController
@RequestMapping("/product")
public class ProductRestAdapter {
    private final SimilarProductsUseCase similarProductsUseCase;

    @Autowired
    public ProductRestAdapter(SimilarProductsUseCase similarProductsUseCase) {
        this.similarProductsUseCase = similarProductsUseCase;
    }

    @GetMapping(value = "/{productId}/similar", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ProductDetailsDTO>> getSimilarProducts(@PathVariable("productId") String productId) {
        validate(productId);
        return ResponseEntity.ok(MAPPER.toProductDetailsDTO(similarProductsUseCase.getSimilarProducts(productId)));
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
