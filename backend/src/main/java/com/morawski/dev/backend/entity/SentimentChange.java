package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.ChangeReason;
import com.morawski.dev.backend.dto.common.Sentiment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a sentiment change event - audit table tracking all sentiment modifications.
 * This includes:
 * - Initial AI classification (AI_INITIAL)
 * - User corrections (USER_CORRECTION)
 * - AI re-analysis (AI_REANALYSIS)
 *
 * User Story US-007: Manual sentiment correction
 * Each change is logged for audit purposes and analytics.
 */
@Entity
@Table(name = "sentiment_changes", indexes = {
        @Index(name = "idx_sentiment_changes_review_id", columnList = "review_id"),
        @Index(name = "idx_sentiment_changes_user_id", columnList = "changed_by_user_id"),
        @Index(name = "idx_sentiment_changes_reason", columnList = "change_reason, changed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentimentChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Old sentiment is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "old_sentiment", nullable = false, length = 20)
    private Sentiment oldSentiment;

    @NotNull(message = "New sentiment is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_sentiment", nullable = false, length = 20)
    private Sentiment newSentiment;

    /**
     * User who made the change (null for AI-generated changes).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @NotNull(message = "Change reason is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "change_reason", nullable = false, length = 30)
    private ChangeReason changeReason;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private Instant changedAt = Instant.now();

    /**
     * Many-to-one relationship with Review.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /**
     * Check if this was a user-initiated change.
     */
    public boolean isUserChange() {
        return changeReason == ChangeReason.USER_CORRECTION;
    }

    /**
     * Check if sentiment actually changed.
     */
    public boolean isSentimentDifferent() {
        return !oldSentiment.equals(newSentiment);
    }
}
