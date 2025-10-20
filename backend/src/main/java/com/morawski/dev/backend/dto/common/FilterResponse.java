package com.morawski.dev.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents active filters applied to review list queries.
 * Returned in ReviewListResponse to show which filters are active.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterResponse {

    /**
     * Filter by specific review source ID
     */
    private Long sourceId;

    /**
     * Filter by sentiment values (POSITIVE, NEGATIVE, NEUTRAL)
     */
    private List<String> sentiment;

    /**
     * Filter by rating values (1-5)
     */
    private List<Integer> rating;

    /**
     * Filter reviews published after this date
     */
    private LocalDate startDate;

    /**
     * Filter reviews published before this date
     */
    private LocalDate endDate;
}
