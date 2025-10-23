package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.review.SentimentChangeResponse;
import com.morawski.dev.backend.entity.SentimentChange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for SentimentChange entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SentimentChangeMapper extends BaseMapper {

    /**
     * Convert SentimentChange entity to SentimentChangeResponse DTO.
     */
    @Mapping(source = "changedByUser.id", target = "changedByUserId")
    SentimentChangeResponse toSentimentChangeResponse(SentimentChange sentimentChange);
}
