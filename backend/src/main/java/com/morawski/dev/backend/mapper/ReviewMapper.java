package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.review.ReviewResponse;
import com.morawski.dev.backend.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Review entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewMapper extends BaseMapper {

    /**
     * Convert Review entity to ReviewResponse DTO.
     * Includes sourceType from parent reviewSource entity.
     */
    @Mapping(source = "id", target = "reviewId")
    @Mapping(source = "reviewSource.id", target = "sourceId")
    @Mapping(source = "reviewSource.sourceType", target = "sourceType")
    ReviewResponse toReviewResponse(Review review);
}
