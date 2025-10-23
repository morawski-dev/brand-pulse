package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a review source (e.g., Google, Facebook, Trustpilot profile).
 * Each brand can have multiple review sources, but free plan limits to 1.
 * Supports both API-based and web scraping authentication methods.
 */
@Entity
@Table(name = "review_sources",
       indexes = {
           @Index(name = "idx_review_sources_brand_id", columnList = "brand_id"),
           @Index(name = "idx_review_sources_next_sync", columnList = "next_scheduled_sync_at"),
           @Index(name = "idx_review_sources_active", columnList = "brand_id, is_active")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_brand_source_external",
                           columnNames = {"brand_id", "source_type", "external_profile_id"})
       })
@SQLRestriction("deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Source type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @NotBlank(message = "Profile URL is required")
    @Column(name = "profile_url", nullable = false, columnDefinition = "TEXT")
    private String profileUrl;

    @NotBlank(message = "External profile ID is required")
    @Column(name = "external_profile_id", nullable = false, length = 255)
    private String externalProfileId;

    @NotNull(message = "Auth method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 20)
    @Builder.Default
    private AuthMethod authMethod = AuthMethod.SCRAPING;

    /**
     * Encrypted credentials stored as JSON (for API authentication).
     * Example: {"api_key": "encrypted_value", "oauth_token": "encrypted_value"}
     */
    @Type(JsonType.class)
    @Column(name = "credentials_encrypted", columnDefinition = "jsonb")
    private Map<String, Object> credentialsEncrypted;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp of the last data synchronization attempt.
     */
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", length = 20)
    private SyncStatus lastSyncStatus;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    /**
     * When the next scheduled sync should occur (CRON-based).
     */
    @Column(name = "next_scheduled_sync_at")
    private Instant nextScheduledSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Many-to-one relationship with Brand.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    /**
     * One-to-many relationship with Review.
     * All reviews fetched from this source.
     */
    @OneToMany(mappedBy = "reviewSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    /**
     * One-to-many relationship with DashboardAggregate.
     */
    @OneToMany(mappedBy = "reviewSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DashboardAggregate> dashboardAggregates = new ArrayList<>();

    /**
     * One-to-many relationship with AISummary.
     */
    @OneToMany(mappedBy = "reviewSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AISummary> aiSummaries = new ArrayList<>();

    /**
     * Helper method to add a review and maintain bidirectional relationship.
     */
    public void addReview(Review review) {
        reviews.add(review);
        review.setReviewSource(this);
    }

    /**
     * Helper method to remove a review and maintain bidirectional relationship.
     */
    public void removeReview(Review review) {
        reviews.remove(review);
        review.setReviewSource(null);
    }

    /**
     * Soft delete the review source.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.isActive = false;
    }

    /**
     * Check if review source is deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Check if last sync was successful.
     */
    public boolean isLastSyncSuccessful() {
        return lastSyncStatus == SyncStatus.SUCCESS;
    }
}
