package com.morawski.dev.backend.util;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.entity.Review;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            return "Brak dostępnych opinii do analizy.";
        }

        // Graceful degradation: without a real API key, return a locally computed
        // statistics-based summary instead of calling (and failing) the AI service.
        if (!isConfigured()) {
            log.warn("OpenRouter not configured (demo/blank key) - using local fallback summary");
            return buildFallbackSummary(reviews);
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
                log.warn("Empty response from AI service - using fallback summary");
                return buildFallbackSummary(reviews);
            }

            // Parse response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("No choices in AI response - using fallback summary");
                return buildFallbackSummary(reviews);
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
            // Don't fail the caller (e.g. post-import summary generation) on a transient
            // AI outage - fall back to a locally computed summary.
            log.error("Failed to generate AI summary, using fallback: {}", e.getMessage(), e);
            return buildFallbackSummary(reviews);
        }
    }

    /**
     * Whether the OpenRouter client is configured with a usable API key.
     * Returns false for blank or the demo placeholder key, in which case callers
     * should fall back to local heuristics rather than calling the API.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("sk-demo-xyz123");
    }

    /**
     * Result of AI sentiment analysis for a single review.
     */
    public record SentimentAnalysis(Sentiment sentiment, BigDecimal confidence) {}

    /**
     * Analyze the sentiment of a single review's text using the AI model.
     *
     * Returns {@link Optional#empty()} when the client is not configured or the
     * AI call/parse fails, so callers can fall back to a rating-based heuristic.
     *
     * @param content review text
     * @return Optional sentiment analysis (sentiment + confidence)
     */
    public Optional<SentimentAnalysis> analyzeSentiment(String content) {
        if (!isConfigured() || content == null || content.isBlank()) {
            return Optional.empty();
        }

        try {
            String prompt = """
                Sklasyfikuj sentyment poniższej opinii klienta jako jedno słowo:
                POSITIVE, NEUTRAL albo NEGATIVE. Odpowiedz wyłącznie tym jednym słowem.

                Opinia:
                """ + StringUtils.truncate(content, 1000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("max_tokens", 5);
            requestBody.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/chat/completions", HttpMethod.POST, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return Optional.empty();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return Optional.empty();
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String answer = ((String) message.get("content")).trim().toUpperCase();

            Sentiment sentiment;
            if (answer.contains("POSITIVE")) sentiment = Sentiment.POSITIVE;
            else if (answer.contains("NEGATIVE")) sentiment = Sentiment.NEGATIVE;
            else if (answer.contains("NEUTRAL")) sentiment = Sentiment.NEUTRAL;
            else {
                log.warn("Unrecognized AI sentiment answer: '{}'", answer);
                return Optional.empty();
            }
            // AI gave a definitive classification; assign a high confidence.
            return Optional.of(new SentimentAnalysis(sentiment, BigDecimal.valueOf(0.95)));

        } catch (Exception e) {
            log.warn("AI sentiment analysis failed, caller should fall back: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Build a deterministic, statistics-based Polish summary without calling the AI.
     * Used as a fallback when the AI service is unconfigured or unavailable.
     */
    private String buildFallbackSummary(List<Review> reviews) {
        long total = reviews.size();
        double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long positive = reviews.stream().filter(r -> r.getSentiment() == Sentiment.POSITIVE).count();
        long negative = reviews.stream().filter(r -> r.getSentiment() == Sentiment.NEGATIVE).count();
        long neutral = reviews.stream().filter(r -> r.getSentiment() == Sentiment.NEUTRAL).count();

        double posPct = total > 0 ? positive * 100.0 / total : 0.0;
        double negPct = total > 0 ? negative * 100.0 / total : 0.0;
        double neuPct = total > 0 ? neutral * 100.0 / total : 0.0;

        return String.format(
            "%.0f%% opinii pozytywnych, %.0f%% neutralnych, %.0f%% negatywnych. "
                + "Średnia ocena: %.1f/5 na podstawie %d opinii.",
            posPct, neuPct, negPct, avgRating, total);
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

            Respond with PLAIN TEXT only — no Markdown, no headings (#), no blockquotes (>),
            no bullet lists, no bold (**). Just 2-3 plain sentences.

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