package com.morawski.dev.backend.dto.source;

import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.Map;

/**
 * Request DTO for creating a new review source (US-003 step 2, US-009).
 * Maps to review_sources table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewSourceRequest {

    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    @NotBlank(message = "Profile URL is required")
    @URL(message = "Invalid URL format")
    private String profileUrl;

    @NotBlank(message = "External profile ID is required")
    @Size(max = 255)
    private String externalProfileId;

    @NotNull(message = "Authentication method is required")
    private AuthMethod authMethod;

    /**
     * Encrypted credentials (API keys, OAuth tokens, etc.)
     * Stored in review_sources.credentials_encrypted as JSONB
     */
    private Map<String, Object> credentialsEncrypted;
}
