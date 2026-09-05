package com.herdcommand.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ANIMAL_STATUS_ACTIVE = "animalStatusActive";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(ANIMAL_STATUS_ACTIVE);
    }
}
