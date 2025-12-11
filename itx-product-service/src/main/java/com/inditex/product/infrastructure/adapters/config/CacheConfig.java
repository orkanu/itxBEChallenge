package com.inditex.product.infrastructure.adapters.config;

import com.inditex.product.domain.model.ProductDetails;
import com.inditex.product.infrastructure.adapters.cache.CacheEventLogger;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.time.Duration;
import java.util.List;

import static javax.cache.Caching.getCachingProvider;
import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheEventListenerConfigurationBuilder.newEventListenerConfiguration;
import static org.ehcache.config.builders.ExpiryPolicyBuilder.timeToLiveExpiration;
import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;
import static org.ehcache.config.units.MemoryUnit.MB;
import static org.ehcache.event.EventType.*;
import static org.ehcache.jsr107.Eh107Configuration.fromEhcacheCacheConfiguration;

@org.springframework.context.annotation.Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PRODUCT_LIST = "similarProductsCache";
    public static final String CACHE_PRODUCT = "productCache";
    private final CacheEventLogger listener;

    @Autowired
    public CacheConfig(CacheEventLogger listener) {
        this.listener = listener;
    }

    @Bean
    public Configuration<String, ProductDetails> productDetailsConfiguration() {
        CacheConfigurationBuilder<String, ProductDetails> productDetailsConfiguration = newCacheConfigurationBuilder(
                String.class, ProductDetails.class,
                newResourcePoolsBuilder().offheap(10, MB).build())
                .withExpiry(timeToLiveExpiration(Duration.ofHours(2)))
                .withService(newEventListenerConfiguration(listener, CREATED, EXPIRED, REMOVED)
                        .asynchronous()
                        .unordered()
                        .build());

        return fromEhcacheCacheConfiguration(productDetailsConfiguration);
    }

    @Bean
    public Configuration<String, List> productListConfiguration() {
        CacheConfigurationBuilder<String, List> configuration = newCacheConfigurationBuilder(
                String.class, List.class,
                newResourcePoolsBuilder().offheap(20, MB).build())
                .withExpiry(timeToLiveExpiration(Duration.ofHours(1)))
                .withService(newEventListenerConfiguration(listener, CREATED, EXPIRED, REMOVED)
                        .asynchronous()
                        .unordered()
                        .build());

        return fromEhcacheCacheConfiguration(configuration);
    }

    @Bean
    public CacheManager ehCacheManager(Configuration<String, ProductDetails> productDetailsConfiguration, Configuration<String, List> productListConfiguration) {
        CacheManager cacheManager = getCachingProvider().getCacheManager();
        cacheManager.createCache(CACHE_PRODUCT_LIST, productListConfiguration);
        cacheManager.createCache(CACHE_PRODUCT, productDetailsConfiguration);
        return cacheManager;
    }
}
