package com.inditex.product.application.usecase;

import com.inditex.product.domain.model.ProductDetails;

public class UseCaseHelpers {

    public static ProductDetails buildProductDetails(String id, String name, Double price, boolean availability) {
        ProductDetails d = new ProductDetails();
        d.setId(id);
        d.setName(name);
        d.setPrice(price);
        d.setAvailability(availability);
        return d;
    }
}
