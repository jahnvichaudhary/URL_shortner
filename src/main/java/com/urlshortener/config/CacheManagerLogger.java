package com.urlshortener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheManagerLogger {

    private static final Logger log =
        LoggerFactory.getLogger(CacheManagerLogger.class);

    public CacheManagerLogger(CacheManager cacheManager) {
        log.info("Active CacheManager: {}",
            cacheManager.getClass().getName());
    }
}
