package com.morawski.dev.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user profile.
 * Updates users table fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;
}
