package com.morawski.dev.backend.security;

import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user-specific data from the database during authentication.
 *
 * This service is called by Spring Security during login to:
 * - Fetch user details from the database
 * - Validate credentials
 * - Load user authorities (roles/permissions)
 *
 * API Plan Section 3.2: User Login
 * - Validates credentials against hashed password
 * - Generates JWT token containing userId, email, planType, maxSourcesAllowed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates the user based on the email (username).
     * This method is called by Spring Security's authentication mechanism.
     *
     * API Plan Section 3.2: POST /api/auth/login
     * - User is identified by email (case-insensitive)
     * - Deleted users cannot authenticate
     *
     * @param email the email identifying the user (username in Spring Security terms)
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        // Find user by email (case-insensitive)
        // SQLRestriction on User entity ensures deleted_at IS NULL
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> {
                log.warn("User not found with email: {}", email);
                return new UsernameNotFoundException("User not found with email: " + email);
            });

        // Additional check: ensure account is not deleted
        if (user.isDeleted()) {
            log.warn("Attempt to authenticate deleted user: {}", email);
            throw new UsernameNotFoundException("User account has been deleted");
        }

        log.debug("User loaded successfully: {} (ID: {})", email, user.getId());

        // Convert User entity to CustomUserDetails
        return CustomUserDetails.fromUser(user);
    }

    /**
     * Load user by ID.
     * Used for JWT token validation and authorization checks.
     *
     * @param userId the user ID
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found with ID: {}", userId);
                return new UsernameNotFoundException("User not found with ID: " + userId);
            });

        // Additional check: ensure account is not deleted
        if (user.isDeleted()) {
            log.warn("Attempt to load deleted user: {}", userId);
            throw new UsernameNotFoundException("User account has been deleted");
        }

        log.debug("User loaded successfully: ID {}", userId);

        return CustomUserDetails.fromUser(user);
    }
}
