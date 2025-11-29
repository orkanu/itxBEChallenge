package com.inditex.product.service.mapper;

import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.service.model.ProductDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductDTOMapper {

    ProductDTOMapper MAPPER = Mappers.getMapper(ProductDTOMapper.class);

    List<ProductDetailsDTO> toProductDetailsDTO(List<ProductDetails> productDetails);
}
