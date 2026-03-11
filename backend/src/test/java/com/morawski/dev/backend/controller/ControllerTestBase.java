package com.morawski.dev.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morawski.dev.backend.config.SecurityConfig;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.security.CustomUserDetails;
import com.morawski.dev.backend.security.JwtAuthenticationEntryPoint;
import com.morawski.dev.backend.security.JwtAuthenticationFilter;
import com.morawski.dev.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Base class for controller tests providing common utilities and helper methods.
 * Extends this class to get access to security mocking and JSON conversion utilities.
 *
 * Imports the real {@link SecurityConfig} (plus the JWT filter and entry point) so
 * that @WebMvcTest slices apply the production authorization rules (public auth
 * endpoints permitted, everything else authenticated). The JWT filter receives the
 * mocked {@link JwtTokenProvider}/{@link com.morawski.dev.backend.security.CustomUserDetailsService}
 * and is a no-op when no Authorization header is present, so requests authenticated
 * via {@link #authenticatedUser} are honored.
 */
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
public abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockBean
    protected com.morawski.dev.backend.security.CustomUserDetailsService customUserDetailsService;

    /**
     * Create CustomUserDetails for testing with specified parameters.
     *
     * @param userId User ID
     * @param email User email
     * @param planType User plan type
     * @param maxSourcesAllowed Max sources allowed for user's plan
     * @return CustomUserDetails instance
     */
    protected CustomUserDetails createUserDetails(Long userId, String email, PlanType planType, Integer maxSourcesAllowed) {
        return new CustomUserDetails(
            userId,
            email,
            "$2a$10$hashedPassword",
            planType,
            maxSourcesAllowed,
            true,
            false
        );
    }

    /**
     * Create CustomUserDetails with default values (FREE plan, 1 source allowed).
     *
     * @param userId User ID
     * @param email User email
     * @return CustomUserDetails instance
     */
    protected CustomUserDetails createUserDetails(Long userId, String email) {
        return createUserDetails(userId, email, PlanType.FREE, 1);
    }

    /**
     * Create CustomUserDetails for premium user.
     *
     * @param userId User ID
     * @param email User email
     * @return CustomUserDetails instance
     */
    protected CustomUserDetails createPremiumUserDetails(Long userId, String email) {
        return createUserDetails(userId, email, PlanType.PREMIUM, 10);
    }


    /**
     * Create a RequestPostProcessor for MockMvc that injects CustomUserDetails authentication.
     * Use this with MockMvc requests: .with(authenticatedUser(userId, email))
     *
     * @param userDetails CustomUserDetails to inject
     * @return RequestPostProcessor
     */
    protected RequestPostProcessor authenticatedUser(CustomUserDetails userDetails) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        return authentication(auth);
    }

    /**
     * Create a RequestPostProcessor with default user (FREE plan).
     *
     * @param userId User ID
     * @param email User email
     * @return RequestPostProcessor
     */
    protected RequestPostProcessor authenticatedUser(Long userId, String email) {
        return authenticatedUser(createUserDetails(userId, email));
    }

    /**
     * Create a RequestPostProcessor with custom plan.
     *
     * @param userId User ID
     * @param email User email
     * @param planType Plan type
     * @param maxSourcesAllowed Max sources allowed
     * @return RequestPostProcessor
     */
    protected RequestPostProcessor authenticatedUser(Long userId, String email, PlanType planType, Integer maxSourcesAllowed) {
        return authenticatedUser(createUserDetails(userId, email, planType, maxSourcesAllowed));
    }

    /**
     * Convert object to JSON string.
     *
     * @param obj Object to convert
     * @return JSON string
     */
    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Clear security context after test.
     */
    protected void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected void setupAuthenticatedUser(long userId, String email) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, email, "password", PlanType.FREE, 5, true, false);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
