package com.morawski.dev.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JPA Auditing configuration for automatic timestamp management.
 *
 * Enables automatic population of audit fields:
 * - @CreatedDate: Set when entity is first persisted
 * - @LastModifiedDate: Updated on every entity modification
 * - @CreatedBy: User who created the entity (requires AuditorAware)
 * - @LastModifiedBy: User who last modified the entity (requires AuditorAware)
 *
 * API Plan Section 13.2: Business Logic Rules
 * Used for tracking:
 * - Entity creation timestamps (created_at)
 * - Entity modification timestamps (updated_at)
 * - Audit trail for sentiment changes
 * - Activity logging timestamps
 *
 * Usage in entities:
 * <pre>
 * {@code
 * @EntityListeners(AuditingEntityListener.class)
 * public class User {
 *     @CreatedDate
 *     private LocalDateTime createdAt;
 *
 *     @LastModifiedDate
 *     private LocalDateTime updatedAt;
 * }
 * }
 * </pre>
 */
@Slf4j
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class AuditConfig {

    /**
     * Date time provider for JPA auditing.
     *
     * Returns current LocalDateTime for @CreatedDate and @LastModifiedDate fields.
     * Using LocalDateTime instead of Instant for consistency with database schema.
     *
     * @return DateTimeProvider that supplies current LocalDateTime
     */
    @Bean
    public DateTimeProvider dateTimeProvider() {
        log.info("Configuring JPA auditing with LocalDateTime provider");
        return () -> Optional.of(LocalDateTime.now());
    }

    /**
     * Auditor aware bean for @CreatedBy and @LastModifiedBy.
     *
     * TODO: Implement when user context is needed for audit fields.
     *
     * Example implementation:
     * <pre>
     * {@code
     * @Bean
     * public AuditorAware<Long> auditorAware() {
     *     return () -> {
     *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *         if (auth == null || !auth.isAuthenticated()) {
     *             return Optional.empty();
     *         }
     *         UserDetails userDetails = (UserDetails) auth.getPrincipal();
     *         return Optional.of(userDetails.getUserId());
     *     };
     * }
     * }
     * </pre>
     *
     * Current implementation: Not needed for MVP as we track user ID manually
     * in business logic (e.g., sentiment_changes.changed_by_user_id).
     */

    // Note: AuditorAware implementation will be added when needed for @CreatedBy/@LastModifiedBy
    // Currently, user tracking is handled explicitly in service layer via JWT token
}