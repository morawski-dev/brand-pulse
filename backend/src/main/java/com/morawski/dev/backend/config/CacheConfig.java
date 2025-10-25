package com.morawski.dev.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for BrandPulse application.
 *
 * Uses Caffeine cache implementation for high performance in-memory caching.
 * Critical for meeting <4 second dashboard load time requirement (API Plan Section 15.1).
 *
 * Cache Strategy:
 * - Dashboard aggregates: 10 minute TTL
 * - AI summaries: 24 hour TTL (managed by AI service)
 * - Review lists: 5 minute TTL
 *
 * Cache Keys (API Plan Section 15.1):
 * - dashboard:brand:{brandId}
 * - summary:source:{sourceId}
 * - reviews:brand:{brandId}:filters:{hash}
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with default settings.
     *
     * API Plan Section 15.1:
     * - TTL: 10 minutes (configurable)
     * - Max Size: 500 entries
     * - Eviction: LRU (Least Recently Used)
     *
     * @return Configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("Configuring Caffeine cache manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "dashboard",      // Dashboard aggregates cache
            "summaries",      // AI summaries cache
            "reviews",        // Review lists cache
            "sources",        // Review sources cache
            "brands"          // Brands cache
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        log.info("Cache manager configured with {} cache names", cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * Caffeine cache builder with performance settings.
     *
     * Configuration:
     * - maximumSize: 500 entries (prevents memory overflow)
     * - expireAfterWrite: 10 minutes (TTL)
     * - recordStats: enabled (for monitoring cache hit/miss rates via Actuator)
     *
     * @return Caffeine builder
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats();
    }

    /**
     * Specialized cache for AI summaries with 24-hour TTL.
     * Separate configuration to reduce OpenRouter.ai API costs.
     *
     * API Plan Section 13.2: AI Summary Caching
     *
     * @return Caffeine builder for AI summaries
     */
    @Bean("aiSummaryCacheBuilder")
    public Caffeine<Object, Object> aiSummaryCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats();
    }
}