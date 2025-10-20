package com.morawski.dev.backend.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating brand information.
 * Updates brands table fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBrandRequest {

    @NotBlank(message = "Brand name is required")
    @Size(min = 1, max = 255, message = "Brand name must be 1-255 characters")
    private String name;
}
