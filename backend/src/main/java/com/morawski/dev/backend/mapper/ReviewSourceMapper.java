package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.source.ReviewSourceResponse;
import com.morawski.dev.backend.dto.source.ReviewSourceSummary;
import com.morawski.dev.backend.entity.ReviewSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for ReviewSource entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewSourceMapper extends BaseMapper {

    /**
     * Convert ReviewSource entity to ReviewSourceResponse DTO.
     * Excludes sensitive credentialsEncrypted field for security.
     */
    @Mapping(source = "id", target = "sourceId")
    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(target = "importJobId", ignore = true)
    @Mapping(target = "importStatus", ignore = true)
    ReviewSourceResponse toReviewSourceResponse(ReviewSource reviewSource);

    /**
     * Convert ReviewSource entity to ReviewSourceSummary DTO.
     * Used in BrandDetailResponse to show basic source info.
     */
    @Mapping(source = "id", target = "sourceId")
    ReviewSourceSummary toReviewSourceSummary(ReviewSource reviewSource);
}
