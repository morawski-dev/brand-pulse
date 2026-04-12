package com.morawski.dev.backend.service.sync;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Internal representation of a single Google review, decoupled from the source
 * (real Places API or mock). Produced by {@link GooglePlacesClient} and consumed
 * by {@link GoogleReviewSyncHandler}.
 */
@Data
@AllArgsConstructor
public class GoogleReviewDTO {
    private String externalId;
    private String content;
    private String authorName;
    private Short rating;
    private Instant publishedAt;
}
