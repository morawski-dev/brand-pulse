package com.morawski.dev.backend.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new brand (US-003 step 1).
 * Maps to brands table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBrandRequest {

    @NotBlank(message = "Brand name is required")
    @Size(min = 1, max = 255, message = "Brand name must be 1-255 characters")
    private String name;
}
