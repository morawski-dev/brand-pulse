package com.morawski.dev.backend.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * OpenRouter.ai API configuration for AI-powered sentiment analysis and summarization.
 *
 * OpenRouter.ai provides access to multiple AI models (OpenAI, Anthropic, Google)
 * with cost control via API key limits.
 *
 * API Plan: AI Integration
 * Provider: Openrouter.ai
 * Rationale: Access to multiple models (OpenAI, Anthropic, Google) with cost control
 *
 * AI Tasks:
 * - Sentiment classification per review (POSITIVE/NEGATIVE/NEUTRAL)
 * - Text summarization per source (e.g., "75% positive; customers praise speed but complain about prices")
 */
@Slf4j
@Configuration
@Getter
public class OpenRouterConfig {

    /**
     * OpenRouter API base URL.
     * Default: https://openrouter.ai/api/v1/chat/completions
     */
    @Value("${openrouter.api.url}")
    private String apiUrl;

    /**
     * OpenRouter API key for authentication.
     * IMPORTANT: Should be stored in environment variable or secret manager in production.
     */
    @Value("${openrouter.api.key}")
    private String apiKey;

    /**
     * AI model to use for sentiment analysis and summarization.
     *
     * Recommended models:
     * - anthropic/claude-3.5-sonnet (high accuracy, good for complex sentiment)
     * - anthropic/claude-3-haiku (fast, cost-effective for high volume)
     * - openai/gpt-4o (alternative option)
     *
     * Current configuration: anthropic/claude-3.5-sonnet
     */
    @Value("${openrouter.model}")
    private String model;

    /**
     * Timeout for API calls in milliseconds.
     * Default: 10000ms (10 seconds)
     *
     * Configured separately from RestTemplate timeout to handle long-running
     * AI operations (especially for text summarization).
     */
    @Value("${openrouter.timeout.ms:10000}")
    private Integer timeoutMs;

    /**
     * Log configuration on bean initialization.
     * Masks API key for security.
     */
    public void logConfiguration() {
        log.info("OpenRouter AI Configuration:");
        log.info("  API URL: {}", apiUrl);
        log.info("  Model: {}", model);
        log.info("  Timeout: {}ms", timeoutMs);
        log.info("  API Key: {}***{}",
            apiKey.substring(0, Math.min(7, apiKey.length())),
            apiKey.length() > 10 ? apiKey.substring(apiKey.length() - 3) : "***"
        );
    }

    /**
     * Get authorization header value for OpenRouter API requests.
     *
     * @return Bearer token format: "Bearer sk-demo-xyz123"
     */
    public String getAuthorizationHeader() {
        return "Bearer " + apiKey;
    }

    /**
     * Validate configuration on startup.
     * Throws exception if critical configuration is missing.
     *
     * @throws IllegalStateException if API key or URL is not configured
     */
    public void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("sk-demo-xyz123")) {
            log.warn("OpenRouter API key is not configured or using demo value. AI features will not work in production.");
        }

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("OpenRouter API URL must be configured");
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException("OpenRouter model must be configured");
        }

        log.info("OpenRouter configuration validated successfully");
    }
}