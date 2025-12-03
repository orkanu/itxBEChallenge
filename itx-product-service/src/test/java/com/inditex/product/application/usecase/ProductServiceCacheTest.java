package com.inditex.product.application.usecase;

import com.inditex.product.application.ports.input.SimilarProductsUseCase;
import com.inditex.product.application.ports.output.SimilarProducts;
import com.inditex.product.domain.model.ProductDetails;
import com.inditex.product.domain.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.inditex.product.application.usecase.UseCaseHelpers.buildProductDetails;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ProductService.class, ProductServiceCacheTest.MockConfig.class, ProductServiceCacheTest.TestCacheConfig.class})
class ProductServiceCacheTest {

    private static final String PRODUCT_A = "A";
    private static final String PRODUCT_B = "B";

    @Autowired
    private SimilarProductsUseCase useCase;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SimilarProducts similarProducts;

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache("similarProductsCache");
        if (cache != null) cache.clear();
        Mockito.reset(similarProducts);
    }

    @Test
    @DisplayName("Cache hit: repeated call with same productId does not call client again")
    void cacheHitSameId() {
        when(similarProducts.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1", "2"));
        when(similarProducts.getProductById("1")).thenReturn(buildProductDetails("1", "P1", 10.0, true));
        when(similarProducts.getProductById("2")).thenReturn(buildProductDetails("2", "P2", 20.0, false));

        List<ProductDetails> first = useCase.getSimilarProducts(PRODUCT_A);
        List<ProductDetails> second = useCase.getSimilarProducts(PRODUCT_A); // should be served from cache

        assertThat(first, hasSize(2));
        assertThat(second, hasSize(2));
        assertThat(second.get(0).getId(), is("1"));
        assertThat(second.get(1).getId(), is("2"));

        verify(similarProducts, times(1)).getSimilarProductIds(PRODUCT_A);
        verify(similarProducts, times(1)).getProductById("1");
        verify(similarProducts, times(1)).getProductById("2");
        verifyNoMoreInteractions(similarProducts);
    }

    @Test
    @DisplayName("Cache keying: different productIds are cached separately")
    void cacheSeparatesDifferentIds() {
        when(similarProducts.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1", "2"));
        when(similarProducts.getSimilarProductIds(PRODUCT_B)).thenReturn(List.of("3", "4"));

        when(similarProducts.getProductById("1")).thenReturn(buildProductDetails("1", "P1", 10.0, true));
        when(similarProducts.getProductById("2")).thenReturn(buildProductDetails("2", "P2", 20.0, false));
        when(similarProducts.getProductById("3")).thenReturn(buildProductDetails("3", "P3", 30.0, true));
        when(similarProducts.getProductById("4")).thenReturn(buildProductDetails("4", "P4", 40.0, false));

        List<ProductDetails> a = useCase.getSimilarProducts(PRODUCT_A);
        List<ProductDetails> b = useCase.getSimilarProducts(PRODUCT_B);

        assertThat(a, hasSize(2));
        assertThat(b, hasSize(2));
        assertThat(a.stream().map(ProductDetails::getId).toList(), contains("1", "2"));
        assertThat(b.stream().map(ProductDetails::getId).toList(), contains("3", "4"));

        verify(similarProducts, times(1)).getSimilarProductIds(PRODUCT_A);
        verify(similarProducts, times(1)).getSimilarProductIds(PRODUCT_B);
        verify(similarProducts, times(1)).getProductById("1");
        verify(similarProducts, times(1)).getProductById("2");
        verify(similarProducts, times(1)).getProductById("3");
        verify(similarProducts, times(1)).getProductById("4");
        verifyNoMoreInteractions(similarProducts);
    }

    @Test
    @DisplayName("Cache eviction: clearing cache forces re-fetch from client")
    void cacheEvictionForcesReload() {
        when(similarProducts.getSimilarProductIds(PRODUCT_A)).thenReturn(List.of("1"));
        when(similarProducts.getProductById("1")).thenReturn(buildProductDetails("1", "P1", 10.0, true));

        // First call populates cache
        List<ProductDetails> first = useCase.getSimilarProducts(PRODUCT_A);
        assertThat(first, hasSize(1));

        // Simulate clearing the cache (i.e. TTL reached)
        Cache cache = cacheManager.getCache("similarProductsCache");
        if (cache != null) cache.clear();

        // The second call should use the client again
        List<ProductDetails> second = useCase.getSimilarProducts(PRODUCT_A);
        assertThat(second, hasSize(1));

        verify(similarProducts, times(2)).getSimilarProductIds(PRODUCT_A);
        verify(similarProducts, times(2)).getProductById("1");
        verifyNoMoreInteractions(similarProducts);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        SimilarProducts productClient() {
            return Mockito.mock(SimilarProducts.class);
        }

        @Bean(name = "resilienceTaskExecutor")
        ThreadPoolTaskExecutor resilienceTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(4);
            executor.setThreadNamePrefix("test-resilience-");
            executor.initialize();
            return executor;
        }

        @Bean
        CircuitBreaker circuitBreaker() {
            CircuitBreaker cb = Mockito.mock(CircuitBreaker.class);
            Mockito.when(cb.run(any(Supplier.class), any(Function.class)))
                    .thenAnswer(invocation -> {
                        Supplier<?> supplier = invocation.getArgument(0);
                        try {
                            return supplier.get();
                        } catch (Throwable t) {
                            Function<Throwable, ?> fallback = invocation.getArgument(1);
                            return fallback.apply(t);
                        }
                    });
            return cb;
        }

        @Bean
        CircuitBreakerFactory<?, ?> circuitBreakerFactory(CircuitBreaker circuitBreaker) {
            CircuitBreakerFactory<?, ?> factory = Mockito.mock(CircuitBreakerFactory.class);
            Mockito.when(factory.create("productClient")).thenReturn(circuitBreaker);
            return factory;
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
