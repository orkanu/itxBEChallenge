package com.inditex.product.application.cache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CacheEventLogger implements CacheEventListener<Object, Object> {

    private final Logger logger = LoggerFactory.getLogger(CacheEventLogger.class);

    @Override
    public void onEvent(
            CacheEvent<? extends Object, ? extends Object> cacheEvent) {
        logger.info("Cache event occurred for key: {}, old value: {}, new value: {}",
                cacheEvent.getKey(), cacheEvent.getOldValue(), cacheEvent.getNewValue());
    }
}
