package com.morawski.dev.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity representing pre-calculated aggregates for fast dashboard loading.
 * Aggregates are calculated per review source and per day.
 * This improves dashboard performance by avoiding real-time calculations.
 *
 * Non-functional requirement: Dashboard must load in <4 seconds.
 */
@Entity
@Table(name = "dashboard_aggregates",
       indexes = {
           @Index(name = "idx_dashboard_aggregates_source_date", columnList = "review_source_id, date"),
           @Index(name = "idx_dashboard_aggregates_last_calc", columnList = "last_calculated_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_dashboard_source_date",
                           columnNames = {"review_source_id", "date"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @Min(value = 0, message = "Total reviews must be at least 0")
    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @DecimalMin(value = "1.00", message = "Average rating must be at least 1.00")
    @DecimalMax(value = "5.00", message = "Average rating must not exceed 5.00")
    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Min(value = 0, message = "Positive count must be at least 0")
    @Column(name = "positive_count", nullable = false)
    @Builder.Default
    private Integer positiveCount = 0;

    @Min(value = 0, message = "Negative count must be at least 0")
    @Column(name = "negative_count", nullable = false)
    @Builder.Default
    private Integer negativeCount = 0;

    @Min(value = 0, message = "Neutral count must be at least 0")
    @Column(name = "neutral_count", nullable = false)
    @Builder.Default
    private Integer neutralCount = 0;

    @Column(name = "last_calculated_at", nullable = false)
    @Builder.Default
    private Instant lastCalculatedAt = Instant.now();

    /**
     * Many-to-one relationship with ReviewSource.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_source_id", nullable = false)
    private ReviewSource reviewSource;

    /**
     * Calculate positive percentage.
     */
    public double getPositivePercentage() {
        if (totalReviews == 0) return 0.0;
        return (positiveCount * 100.0) / totalReviews;
    }

    /**
     * Calculate negative percentage.
     */
    public double getNegativePercentage() {
        if (totalReviews == 0) return 0.0;
        return (negativeCount * 100.0) / totalReviews;
    }

    /**
     * Calculate neutral percentage.
     */
    public double getNeutralPercentage() {
        if (totalReviews == 0) return 0.0;
        return (neutralCount * 100.0) / totalReviews;
    }

    /**
     * Check if aggregates need recalculation (older than 1 hour).
     */
    public boolean needsRecalculation() {
        return lastCalculatedAt.isBefore(Instant.now().minusSeconds(3600));
    }
}
