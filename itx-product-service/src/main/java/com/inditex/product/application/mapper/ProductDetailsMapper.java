package com.inditex.product.application.mapper;

import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.client.model.SimuladoProductDetails;
import com.inditex.product.service.model.ProductDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductDetailsMapper {

    ProductDetailsMapper MAPPER = Mappers.getMapper(ProductDetailsMapper.class);

    List<ProductDetails> toProductDetails(List<SimuladoProductDetails> productDetails);
}
