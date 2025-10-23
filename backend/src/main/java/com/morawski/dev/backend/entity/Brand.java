package com.morawski.dev.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a brand/company in the BrandPulse system.
 * In MVP version, each user can manage only one brand.
 * Supports soft delete pattern.
 */
@Entity
@Table(name = "brands", indexes = {
        @Index(name = "idx_brands_user_id", columnList = "user_id"),
        @Index(name = "idx_brands_last_manual_refresh", columnList = "last_manual_refresh_at")
})
@SQLRestriction("deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Brand name is required")
    @Size(max = 255, message = "Brand name must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Timestamp of the last manual refresh triggered by user.
     * Used to enforce 24h cooldown between manual refreshes.
     */
    @Column(name = "last_manual_refresh_at")
    private Instant lastManualRefreshAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * One-to-one relationship with User (MVP: one brand per user).
     */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * One-to-many relationship with ReviewSource.
     * A brand can have multiple review sources (Google, Facebook, Trustpilot).
     * Free plan limits this to 1 source.
     */
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewSource> reviewSources = new ArrayList<>();

    /**
     * Helper method to add a review source and maintain bidirectional relationship.
     */
    public void addReviewSource(ReviewSource reviewSource) {
        reviewSources.add(reviewSource);
        reviewSource.setBrand(this);
    }

    /**
     * Helper method to remove a review source and maintain bidirectional relationship.
     */
    public void removeReviewSource(ReviewSource reviewSource) {
        reviewSources.remove(reviewSource);
        reviewSource.setBrand(null);
    }

    /**
     * Soft delete the brand.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Alias method for getting brand name.
     * Used for consistency with DTOs.
     */
    public String getBrandName() {
        return this.name;
    }

    /**
     * Check if brand is deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Check if manual refresh is allowed (24h cooldown).
     * User Story US-008: Manual data refresh with 24h cooldown.
     */
    public boolean canManualRefresh() {
        if (lastManualRefreshAt == null) {
            return true;
        }
        Duration timeSinceLastRefresh = Duration.between(lastManualRefreshAt, Instant.now());
        return timeSinceLastRefresh.toHours() >= 24;
    }

    /**
     * Get remaining time until next manual refresh is allowed.
     */
    public Duration getTimeUntilNextManualRefresh() {
        if (lastManualRefreshAt == null) {
            return Duration.ZERO;
        }
        Instant nextAllowedRefresh = lastManualRefreshAt.plus(Duration.ofHours(24));
        Duration remaining = Duration.between(Instant.now(), nextAllowedRefresh);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
