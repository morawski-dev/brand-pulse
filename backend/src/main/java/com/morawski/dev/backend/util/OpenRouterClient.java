package com.morawski.dev.backend.util;

import com.morawski.dev.backend.entity.Review;
import com.morawski.dev.backend.exception.ServiceUnavailableException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for OpenRouter.ai API integration.
 * Handles AI-based text summarization for review analysis.
 *
 * OpenRouter.ai provides access to multiple AI models (Claude, GPT, etc.)
 * with cost control via API key limits.
 *
 * User Story US-004: Dashboard shows AI text summary per source.
 */
@Component
@Slf4j
public class OpenRouterClient {

    private final RestTemplate restTemplate;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Getter
    private int lastTokenCount;

    /**
     * Constructor with RestTemplate injection.
     * RestTemplate is configured in application configuration.
     */
    public OpenRouterClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.lastTokenCount = 0;
    }

    /**
     * Generate AI summary from a list of reviews.
     * Analyzes sentiment distribution, common themes, and trends.
     *
     * Example output:
     * "75% positive reviews. Customers consistently praise the speed of service
     * and fresh ingredients. Main complaints focus on pricing (mentioned in 18 reviews)
     * and limited parking availability (12 reviews)."
     *
     * @param reviews List of reviews to summarize (typically last 100)
     * @return Generated summary text
     * @throws ServiceUnavailableException if AI service is unavailable
     */
    public String generateSummary(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            log.warn("No reviews provided for summary generation");
            return "No reviews available for analysis.";
        }

        log.info("Generating AI summary for {} reviews using model: {}", reviews.size(), model);

        try {
            // Build prompt with review data
            String prompt = buildSummaryPrompt(reviews);

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", Constants.AI_SUMMARY_MAX_TOKENS);
            requestBody.put("temperature", 0.3); // Lower temperature for more focused summaries

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call OpenRouter API
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/chat/completions",
                HttpMethod.POST,
                request,
                Map.class
            );

            // Extract summary from response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ServiceUnavailableException("Empty response from AI service");
            }

            // Parse response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ServiceUnavailableException("No choices in AI response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String summaryText = (String) message.get("content");

            // Extract token usage
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            if (usage != null) {
                this.lastTokenCount = (Integer) usage.get("total_tokens");
                log.info("AI summary generated. Tokens used: {}", lastTokenCount);
            }

            return summaryText.trim();

        } catch (RestClientException e) {
            log.error("Failed to generate AI summary: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("AI service unavailable: " + e.getMessage());
        }
    }

    /**
     * Build prompt for AI summary generation.
     * Includes review counts, ratings, sentiments, and sample content.
     *
     * @param reviews List of reviews to analyze
     * @return Formatted prompt string
     */
    private String buildSummaryPrompt(List<Review> reviews) {
        // Calculate statistics
        long totalReviews = reviews.size();
        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        long positiveCount = reviews.stream()
            .filter(r -> r.getSentiment().name().equals(Constants.SENTIMENT_POSITIVE))
            .count();
        long negativeCount = reviews.stream()
            .filter(r -> r.getSentiment().name().equals(Constants.SENTIMENT_NEGATIVE))
            .count();
        long neutralCount = reviews.stream()
            .filter(r -> r.getSentiment().name().equals(Constants.SENTIMENT_NEUTRAL))
            .count();

        // Sample reviews for context (max 20)
        String reviewSamples = reviews.stream()
            .limit(20)
            .map(r -> String.format("Rating %d (%s): %s",
                r.getRating(),
                r.getSentiment().name(),
                StringUtils.truncate(r.getContent(), 200)))
            .collect(Collectors.joining("\n"));

        // Build prompt
        return String.format(
            """
            Analyze the following customer reviews and generate a concise summary in Polish.

            Statistics:
            - Total reviews: %d
            - Average rating: %.2f/5
            - Positive: %d (%.1f%%)
            - Negative: %d (%.1f%%)
            - Neutral: %d (%.1f%%)

            Sample reviews:
            %s

            Please provide a 2-3 sentence summary that includes:
            1. Overall sentiment percentage
            2. Main positive themes (what customers praise)
            3. Main negative themes (what customers complain about)

            Format: "X%% pozytywnych opinii. Klienci chwalą... Główne skargi dotyczą..."
            """,
            totalReviews,
            avgRating,
            positiveCount, (positiveCount * 100.0 / totalReviews),
            negativeCount, (negativeCount * 100.0 / totalReviews),
            neutralCount, (neutralCount * 100.0 / totalReviews),
            reviewSamples
        );
    }

    /**
     * Get the model name currently in use.
     *
     * @return AI model identifier (e.g., "anthropic/claude-3-haiku")
     */
    public String getModelName() {
        return model;
    }

    /**
     * Health check for AI service availability.
     *
     * @return true if service is reachable
     */
    public boolean isAvailable() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/models",
                HttpMethod.GET,
                request,
                Map.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}