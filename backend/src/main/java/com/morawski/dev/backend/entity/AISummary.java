package com.morawski.dev.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity representing AI-generated text summaries for review sources.
 * Summaries provide quick insights like:
 * "75% positive reviews; customers praise speed but complain about prices"
 *
 * User Story US-004: Dashboard shows AI text summary per source.
 * Summaries are cached with expiration to balance freshness and API costs.
 */
@Entity
@Table(name = "ai_summaries", indexes = {
        @Index(name = "idx_ai_summaries_source_id", columnList = "review_source_id, generated_at"),
        @Index(name = "idx_ai_summaries_valid_until", columnList = "review_source_id, valid_until")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Summary text is required")
    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Size(max = 100, message = "Model name must not exceed 100 characters")
    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Min(value = 0, message = "Token count must be at least 0")
    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    /**
     * When this summary expires and needs regeneration.
     * Typically set to 24-48 hours after generation.
     */
    @Column(name = "valid_until")
    private Instant validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Many-to-one relationship with ReviewSource.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_source_id", nullable = false)
    private ReviewSource reviewSource;

    /**
     * Check if summary is still valid (not expired).
     */
    public boolean isValid() {
        if (validUntil == null) {
            return true;
        }
        return Instant.now().isBefore(validUntil);
    }

    /**
     * Check if summary is expired and needs regeneration.
     */
    public boolean isExpired() {
        return !isValid();
    }

    /**
     * Set validity period (e.g., 24 hours from now).
     */
    public void setValidityPeriod(long hours) {
        this.validUntil = Instant.now().plusSeconds(hours * 3600);
    }
}
