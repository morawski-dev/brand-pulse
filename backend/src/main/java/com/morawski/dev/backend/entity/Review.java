package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.ChangeReason;
import com.morawski.dev.backend.dto.common.Sentiment;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a customer review from an external source.
 * Reviews are fetched from Google, Facebook, or Trustpilot.
 * Sentiment can be changed by users - changes are tracked in sentiment_changes table.
 */
@Entity
@Table(name = "reviews",
       indexes = {
           @Index(name = "idx_reviews_source_id", columnList = "review_source_id"),
           @Index(name = "idx_reviews_published_at", columnList = "published_at"),
           @Index(name = "idx_reviews_sentiment", columnList = "review_source_id, sentiment"),
           @Index(name = "idx_reviews_rating", columnList = "review_source_id, rating"),
           @Index(name = "idx_reviews_negative", columnList = "review_source_id, published_at"),
           @Index(name = "idx_reviews_content_hash", columnList = "content_hash"),
           @Index(name = "idx_reviews_composite_filter", columnList = "review_source_id, sentiment, rating, published_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_review_source_external",
                           columnNames = {"review_source_id", "external_review_id"})
       })
@SQLRestriction("deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "External review ID is required")
    @Size(max = 255, message = "External review ID must not exceed 255 characters")
    @Column(name = "external_review_id", nullable = false, length = 255)
    private String externalReviewId;

    @NotBlank(message = "Content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotBlank(message = "Content hash is required")
    @Size(max = 64, message = "Content hash must not exceed 64 characters")
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Size(max = 255, message = "Author name must not exceed 255 characters")
    @Column(name = "author_name", length = 255)
    private String authorName;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @Column(nullable = false)
    private Short rating;

    /**
     * Current sentiment of the review (can be changed by user).
     * Initial value set by AI, but can be updated through sentiment_changes table.
     */
    @NotNull(message = "Sentiment is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sentiment sentiment;

    /**
     * Confidence score of AI sentiment analysis (0.0 to 1.0).
     * Higher values indicate higher confidence in the classification.
     */
    @DecimalMin(value = "0.0000", message = "Confidence must be at least 0.0")
    @DecimalMax(value = "1.0000", message = "Confidence must not exceed 1.0")
    @Column(name = "sentiment_confidence", precision = 5, scale = 4)
    private BigDecimal sentimentConfidence;

    @NotNull(message = "Published date is required")
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    /**
     * When the review was fetched from external source.
     */
    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private Instant fetchedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Many-to-one relationship with ReviewSource.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_source_id", nullable = false)
    private ReviewSource reviewSource;

    /**
     * One-to-many relationship with SentimentChange.
     * Tracks all sentiment modifications (AI initial + user corrections).
     */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    @Builder.Default
    private List<SentimentChange> sentimentChanges = new ArrayList<>();

    /**
     * Helper method to add a sentiment change and maintain bidirectional relationship.
     */
    public void addSentimentChange(SentimentChange sentimentChange) {
        sentimentChanges.add(sentimentChange);
        sentimentChange.setReview(this);
    }

    /**
     * Soft delete the review.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Check if review is deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Check if sentiment was manually corrected by user.
     */
    public boolean isSentimentManuallyChanged() {
        return sentimentChanges.stream()
                .anyMatch(change -> change.getChangeReason() == ChangeReason.USER_CORRECTION);
    }

    /**
     * Get the most recent sentiment change.
     */
    public SentimentChange getLatestSentimentChange() {
        return sentimentChanges.isEmpty() ? null : sentimentChanges.get(0);
    }
}
