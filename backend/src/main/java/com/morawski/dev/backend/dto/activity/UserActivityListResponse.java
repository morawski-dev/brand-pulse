package com.morawski.dev.backend.dto.activity;

import com.morawski.dev.backend.dto.common.PaginationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for paginated user activity list.
 * Used in GET /api/users/me/activity endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityListResponse {

    /**
     * Paginated list of user activities
     */
    private List<UserActivityResponse> activities;

    /**
     * Pagination metadata
     */
    private PaginationResponse pagination;
}
