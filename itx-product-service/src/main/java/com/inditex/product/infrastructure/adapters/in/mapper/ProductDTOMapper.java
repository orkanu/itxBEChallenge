package com.inditex.product.infrastructure.adapters.in.mapper;

import com.inditex.product.domain.model.ProductDetails;
import com.inditex.product.infrastructure.adapters.in.dto.ProductDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductDTOMapper {

    ProductDTOMapper MAPPER = Mappers.getMapper(ProductDTOMapper.class);

    List<ProductDetailsDTO> toProductDetailsDTO(List<ProductDetails> productDetails);
}
