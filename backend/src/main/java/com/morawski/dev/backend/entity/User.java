package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.PlanType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a user account in the BrandPulse system.
 * Each user can manage one brand in the MVP version.
 * Supports freemium model with FREE and PREMIUM plan types.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_verification_token", columnList = "verification_token"),
        @Index(name = "idx_users_password_reset_token", columnList = "password_reset_token")
})
@SQLRestriction("deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Min(value = 1, message = "Max sources allowed must be at least 1")
    @Column(name = "max_sources_allowed", nullable = false)
    @Builder.Default
    private Integer maxSourcesAllowed = 1;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Size(max = 255, message = "Verification token must not exceed 255 characters")
    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Size(max = 255, message = "Password reset token must not exceed 255 characters")
    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * One-to-one relationship with Brand (MVP: one brand per user).
     * In future versions, this could become one-to-many.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Brand brand;

    /**
     * Soft delete the user account.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Check if user account is deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Check if password reset token is valid.
     */
    public boolean isPasswordResetTokenValid() {
        return passwordResetToken != null
                && passwordResetExpiresAt != null
                && Instant.now().isBefore(passwordResetExpiresAt);
    }
}
