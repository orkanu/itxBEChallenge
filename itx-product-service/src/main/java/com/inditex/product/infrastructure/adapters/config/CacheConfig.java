package com.inditex.product.infrastructure.adapters.config;

import com.inditex.product.infrastructure.adapters.cache.CacheEventLogger;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
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

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE = "similarProductsCache";
    private final CacheEventLogger listener;

    @Autowired
    public CacheConfig(CacheEventLogger listener) {
        this.listener = listener;
    }

    @Bean
    public CacheManager ehCacheManager() {
        CacheManager cacheManager = getCachingProvider().getCacheManager();

        CacheConfigurationBuilder<String, List> configuration = newCacheConfigurationBuilder(
                String.class, List.class,
                newResourcePoolsBuilder().offheap(20, MB).build())
                .withExpiry(timeToLiveExpiration(Duration.ofHours(1)))
                .withService(newEventListenerConfiguration(listener, CREATED, EXPIRED, REMOVED)
                        .asynchronous()
                        .unordered()
                        .build());

        javax.cache.configuration.Configuration<String, List> stringListConfiguration =
                fromEhcacheCacheConfiguration(configuration);

        cacheManager.createCache(CACHE, stringListConfiguration);
        return cacheManager;

    }
}
