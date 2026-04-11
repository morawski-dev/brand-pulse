package com.morawski.dev.backend.service.sync;

import com.morawski.dev.backend.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for the Google Places API (New) — Place Details endpoint.
 *
 * Without a Business Profile we can only use Places API, which returns at most
 * ~5 reviews per place (Google-selected, no pagination, no historical range).
 * The place is identified by its Place ID (e.g. "ChIJ..."), stored as the review
 * source's {@code externalProfileId}.
 *
 * Docs: https://developers.google.com/maps/documentation/places/web-service/place-details
 */
@Component
@Slf4j
public class GooglePlacesClient {

    private final RestTemplate restTemplate;

    @Value("${google.places.api.url}")
    private String apiUrl;

    @Value("${google.places.api.key}")
    private String apiKey;

    public GooglePlacesClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Whether a real Places API key is configured. When false, callers fall back
     * to mock data so the app works without Google credentials.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Fetch up to ~5 reviews for a place via Place Details.
     *
     * @param placeId Google Place ID (e.g. "ChIJ...")
     * @return list of reviews (possibly empty)
     * @throws ExternalServiceException if the API call fails
     */
    @SuppressWarnings("unchecked")
    public List<GoogleReviewDTO> fetchReviews(String placeId) {
        log.info("Fetching reviews from Google Places API for placeId: {}", placeId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey);
        // Reviews are part of the (billable) Enterprise+Atmosphere field set.
        headers.set("X-Goog-FieldMask", "id,displayName,rating,userRatingCount,reviews");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/places/" + placeId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("Empty response body from Google Places for placeId: {}", placeId);
                return List.of();
            }

            List<Map<String, Object>> reviews = (List<Map<String, Object>>) body.get("reviews");
            if (reviews == null || reviews.isEmpty()) {
                log.info("Google Places returned no reviews for placeId: {}", placeId);
                return List.of();
            }

            List<GoogleReviewDTO> result = new ArrayList<>();
            for (Map<String, Object> review : reviews) {
                result.add(mapReview(review));
            }
            log.info("Fetched {} review(s) from Google Places for placeId: {}", result.size(), placeId);
            return result;

        } catch (RestClientException e) {
            log.error("Google Places API call failed for placeId {}: {}", placeId, e.getMessage());
            throw ExternalServiceException.forGoogleApi(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private GoogleReviewDTO mapReview(Map<String, Object> review) {
        // Stable external id: the review resource name "places/{id}/reviews/{id}".
        String name = (String) review.get("name");

        // Prefer originalText (untranslated), fall back to localized text.
        Map<String, Object> textObj = (Map<String, Object>) review.get("originalText");
        if (textObj == null) {
            textObj = (Map<String, Object>) review.get("text");
        }
        String content = textObj != null ? (String) textObj.getOrDefault("text", "") : "";

        Map<String, Object> author = (Map<String, Object>) review.get("authorAttribution");
        String authorName = author != null
            ? (String) author.getOrDefault("displayName", "Anonim")
            : "Anonim";

        short rating = review.get("rating") instanceof Number n ? (short) n.intValue() : 0;

        String publishTime = (String) review.get("publishTime");
        Instant publishedAt = Instant.now();
        if (publishTime != null) {
            try {
                publishedAt = Instant.parse(publishTime);
            } catch (DateTimeParseException ex) {
                log.warn("Could not parse review publishTime '{}', using now()", publishTime);
            }
        }

        String externalId = name != null && !name.isBlank()
            ? name
            : authorName + "|" + publishTime;

        return new GoogleReviewDTO(externalId, content, authorName, rating, publishedAt);
    }
}
