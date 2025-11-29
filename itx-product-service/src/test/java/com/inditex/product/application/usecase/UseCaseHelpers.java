package com.inditex.product.application.usecase;

import com.inditex.product.client.model.SimuladoProductDetails;

public class UseCaseHelpers {

    public static SimuladoProductDetails simuladoProductDetails(String id, String name, Double price, boolean availability) {
        SimuladoProductDetails d = new SimuladoProductDetails();
        d.setId(id);
        d.setName(name);
        d.setPrice(price);
        d.setAvailability(availability);
        return d;
    }
}
