package com.inditex.product.service.controller;

import com.inditex.product.application.ports.input.SimilarProductsUseCase;
import com.inditex.product.domain.model.ProductDetails;
import com.inditex.product.infrastructure.adapters.in.ProductRestAdapter;
import com.inditex.product.infrastructure.adapters.in.dto.ProductDetailsDTO;
import com.inditex.product.infrastructure.adapters.in.exception.RestAdapterExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductRestAdapterTest {

    private static final String PRODUCT_ID = "123";

    @Mock
    private SimilarProductsUseCase similarProductsUseCase;

    private ProductRestAdapter testee;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        this.testee = new ProductRestAdapter(similarProductsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(testee)
                .setControllerAdvice(new RestAdapterExceptionHandler())
                .build();
    }

    // Here I'm using "mockMvc" to test exception handling
    @Test
    void shouldReturnBadRequestWhenProductIdIsInvalid() throws Exception {
        mockMvc.perform(get("/product/invalid/similar"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid productId"));
    }

    @Test
    void shouldReturnSimilarProducts() {
        List<ProductDetails> productDetailsList = List.of(new ProductDetails() {{
            setId("234");
            setName("Product 2");
            setPrice(100.0);
            setAvailability(true);
        }});
        given(similarProductsUseCase.getSimilarProducts(PRODUCT_ID)).willReturn(productDetailsList);
        final ResponseEntity<List<ProductDetailsDTO>> similarProducts = testee.getSimilarProducts(PRODUCT_ID);

        then(similarProductsUseCase).should().getSimilarProducts(PRODUCT_ID);

        assertThat(similarProducts.getBody(), notNullValue());
        assertThat(similarProducts.getBody().size(), is(1));
        ProductDetailsDTO dto = similarProducts.getBody().get(0);
        assertThat(dto.getId(), is("234"));
        assertThat(dto.getName(), is("Product 2"));
        assertThat(dto.getPrice(), is(100.0));
        assertThat(dto.isAvailability(), is(true));
    }
}