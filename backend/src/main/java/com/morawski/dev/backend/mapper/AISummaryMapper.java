package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.dashboard.AISummaryResponse;
import com.morawski.dev.backend.entity.AISummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for AISummary entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AISummaryMapper extends BaseMapper {

    /**
     * Convert AISummary entity to AISummaryResponse DTO.
     * Maps field names between entity and DTO conventions.
     */
    @Mapping(source = "id", target = "summaryId")
    @Mapping(source = "summaryText", target = "text")
    @Mapping(source = "reviewSource.id", target = "sourceId")
    AISummaryResponse toAISummaryResponse(AISummary aiSummary);
}
