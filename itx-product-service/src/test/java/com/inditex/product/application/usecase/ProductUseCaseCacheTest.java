package com.inditex.product.application.usecase;

import com.inditex.product.application.model.ProductDetails;
import com.inditex.product.application.ProductApp;
import com.inditex.product.client.ProductClient;
import com.inditex.product.client.model.SimuladoProductDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;

import static com.inditex.product.application.usecase.UseCaseHelpers.simuladoProductDetails;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ProductUseCase.class, ProductUseCaseCacheTest.MockConfig.class, ProductUseCaseCacheTest.TestCacheConfig.class})
class ProductUseCaseCacheTest {

    private static final String PRODUCT_A = "A";
    private static final String PRODUCT_B = "B";

    @Autowired
    private ProductApp useCase;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductClient productClient;

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache("similarProductsCache");
        if (cache != null) cache.clear();
        Mockito.reset(productClient);
    }

    @Test
    @DisplayName("Cache hit: repeated call with same productId does not call client again")
    void cacheHitSameId() {
        when(productClient.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1", "2"));
        when(productClient.getProductById("1")).thenReturn(simuladoProductDetails("1", "P1", 10.0, true));
        when(productClient.getProductById("2")).thenReturn(simuladoProductDetails("2", "P2", 20.0, false));

        List<ProductDetails> first = useCase.getSimilarProducts(PRODUCT_A);
        List<ProductDetails> second = useCase.getSimilarProducts(PRODUCT_A); // should be served from cache

        assertThat(first, hasSize(2));
        assertThat(second, hasSize(2));
        assertThat(second.get(0).getId(), is("1"));
        assertThat(second.get(1).getId(), is("2"));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_A);
        verify(productClient, times(1)).getProductById("1");
        verify(productClient, times(1)).getProductById("2");
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Cache keying: different productIds are cached separately")
    void cacheSeparatesDifferentIds() {
        when(productClient.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1", "2"));
        when(productClient.getSimilarProductIds(PRODUCT_B)).thenReturn(List.of("3", "4"));

        when(productClient.getProductById("1")).thenReturn(simuladoProductDetails("1", "P1", 10.0, true));
        when(productClient.getProductById("2")).thenReturn(simuladoProductDetails("2", "P2", 20.0, false));
        when(productClient.getProductById("3")).thenReturn(simuladoProductDetails("3", "P3", 30.0, true));
        when(productClient.getProductById("4")).thenReturn(simuladoProductDetails("4", "P4", 40.0, false));

        List<ProductDetails> a = useCase.getSimilarProducts(PRODUCT_A);
        List<ProductDetails> b = useCase.getSimilarProducts(PRODUCT_B);

        assertThat(a, hasSize(2));
        assertThat(b, hasSize(2));
        assertThat(a.stream().map(ProductDetails::getId).toList(), contains("1", "2"));
        assertThat(b.stream().map(ProductDetails::getId).toList(), contains("3", "4"));

        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_A);
        verify(productClient, times(1)).getSimilarProductIds(PRODUCT_B);
        verify(productClient, times(1)).getProductById("1");
        verify(productClient, times(1)).getProductById("2");
        verify(productClient, times(1)).getProductById("3");
        verify(productClient, times(1)).getProductById("4");
        verifyNoMoreInteractions(productClient);
    }

    @Test
    @DisplayName("Cache eviction: clearing cache forces re-fetch from client")
    void cacheEvictionForcesReload() {
        when(productClient.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1"));
        when(productClient.getProductById("1")).thenReturn(simuladoProductDetails("1", "P1", 10.0, true));

        // First call populates cache
        List<ProductDetails> first = useCase.getSimilarProducts(PRODUCT_A);
        assertThat(first, hasSize(1));

        // Simulate clearing the cache (i.e. TTL reached)
        Cache cache = cacheManager.getCache("similarProductsCache");
        if (cache != null) cache.clear();

        // The second call should use the client again
        List<ProductDetails> second = useCase.getSimilarProducts(PRODUCT_A);
        assertThat(second, hasSize(1));

        verify(productClient, times(2)).getSimilarProductIds(PRODUCT_A);
        verify(productClient, times(2)).getProductById("1");
        verifyNoMoreInteractions(productClient);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        ProductClient productClient() {
            return Mockito.mock(ProductClient.class);
        }
    }

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("similarProductsCache");
        }
    }
}
