package com.inditex.product.application.usecase;

import com.inditex.product.domain.model.ports.output.SimilarProducts;
import com.inditex.product.shared.exception.CircuitBreakerException;
import com.inditex.product.shared.exception.ProductNotFoundException;
import com.inditex.product.domain.model.ProductDetails;
import com.inditex.product.application.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.inditex.product.application.usecase.UseCaseHelpers.buildProductDetails;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String PRODUCT_ID = "123";

    @Mock
    private SimilarProducts productClient;

    @InjectMocks
    private ProductService useCase;

    @Test
    @DisplayName("Returns empty list when similar product IDs is null")
    void shouldReturnEmptyListWhenSimilarIdsIsNull() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(null);

        List<ProductDetails> result = useCase.getSimilarProducts(PRODUCT_ID);

        assertThat(result, is(empty()));
        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, never()).getProductById(anyString());
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Returns empty list when similar product IDs is empty")
    void shouldReturnEmptyListWhenSimilarIdsIsEmpty() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of());

        List<ProductDetails> result = useCase.getSimilarProducts(PRODUCT_ID);

        assertThat(result, is(empty()));
        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, never()).getProductById(anyString());
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Happy path: maps non-null product details for each similar ID")
    void shouldMapAllProducts() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("1", "2"));

        when(productClient.getProductById("1")).thenReturn(buildProductDetails("1", "P1", 10.0, true));
        when(productClient.getProductById("2")).thenReturn(buildProductDetails("2", "P2", 20.0, false));

        List<ProductDetails> result = useCase.getSimilarProducts(PRODUCT_ID);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getId(), is("1"));
        assertThat(result.get(0).getName(), is("P1"));
        assertThat(result.get(0).getPrice(), is(10.0));
        assertThat(result.get(0).isAvailability(), is(true));

        assertThat(result.get(1).getId(), is("2"));
        assertThat(result.get(1).getName(), is("P2"));
        assertThat(result.get(1).getPrice(), is(20.0));
        assertThat(result.get(1).isAvailability(), is(false));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("1");
        verify(productClient, times(1)).getProductById("2");
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Null product details from client are filtered out")
    void shouldFilterNullProductDetails() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("1", "2"));

        when(productClient.getProductById("1")).thenReturn(buildProductDetails("1", "P1", 10.0, true));
        when(productClient.getProductById("2")).thenReturn(null);

        List<ProductDetails> result = useCase.getSimilarProducts(PRODUCT_ID);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getId(), is("1"));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("1");
        verify(productClient, times(1)).getProductById("2");
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Client exception during product fetch causes BreakerApplicationException to propagate")
    void shouldPropagateBreakerExceptionOnClientFailure() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("1", "2"));
        when(productClient.getProductById("1")).thenThrow(new CircuitBreakerException("Something went wrong"));

        assertThrows(CircuitBreakerException.class, () -> useCase.getSimilarProducts(PRODUCT_ID));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("1");
        verify(productClient, never()).getProductById("2");
    }

    @Test
    @DisplayName("Client 404 Not Found leads to ProductNotFoundException propagation")
    void shouldPropagateBreakerExceptionOn404() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("404"));
        when(productClient.getProductById("404")).thenThrow(new ProductNotFoundException("404 Not Found"));

        assertThrows(ProductNotFoundException.class, () -> useCase.getSimilarProducts(PRODUCT_ID));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("404");
        verifyNoMoreInteractions(productClient);
    }

}
