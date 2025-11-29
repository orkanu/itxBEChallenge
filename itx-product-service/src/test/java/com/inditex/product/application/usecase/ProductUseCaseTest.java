package com.inditex.product.application.usecase;

import com.inditex.product.application.exception.ApplicationException;
import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.client.ProductClient;
import com.inditex.product.client.exception.ClientException;
import com.inditex.product.client.model.SimuladoProductDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.inditex.product.application.usecase.UseCaseHelpers.simuladoProductDetails;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseTest {

    private static final String PRODUCT_ID = "123";
    @Mock
    private ProductClient productClient;

    @InjectMocks
    private ProductUseCase useCase;

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

        when(productClient.getProductById("1")).thenReturn(simuladoProductDetails("1", "P1", 10.0, true));
        when(productClient.getProductById("2")).thenReturn(simuladoProductDetails("2", "P2", 20.0, false));

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

        when(productClient.getProductById("1")).thenReturn(simuladoProductDetails("1", "P1", 10.0, true));
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
    @DisplayName("Client throws generic exception during product fetch -> ApplicationException")
    void shouldCatchClientExceptionAndWrapItAsApplicationException() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("1", "2"));
        when(productClient.getProductById("1")).thenThrow(new ClientException("Something went wrong"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.getSimilarProducts(PRODUCT_ID));
        assertThat(ex.getMessage(), is("Something went wrong"));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("1");
        // Stream short-circuit is not guaranteed if there is an exception during iteration but, due to eager collection,
        // it will stop on the first thrown exception
        verify(productClient, Mockito.never()).getProductById("2");
    }

    @Test
    @DisplayName("Client 404 Not Found -> ApplicationException propagated with message")
    void shouldWrapClient404NotFoundExceptionAsApplicationException() {
        when(productClient.getSimilarProductIds(PRODUCT_ID)).thenReturn(List.of("404"));
        when(productClient.getProductById("404")).thenThrow(new ClientException("404 Not Found"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.getSimilarProducts(PRODUCT_ID));
        assertThat(ex.getMessage(), is("404 Not Found"));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_ID);
        verify(productClient, times(1)).getProductById("404");
        verifyNoMoreInteractions(productClient);
    }

}
