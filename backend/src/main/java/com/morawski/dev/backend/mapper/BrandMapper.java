package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.brand.BrandResponse;
import com.morawski.dev.backend.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Brand entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BrandMapper extends BaseMapper {

    /**
     * Convert Brand entity to BrandResponse DTO.
     * Calculates sourceCount from reviewSources collection size.
     */
    @Mapping(source = "id", target = "brandId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "sourceCount", expression = "java(brand.getReviewSources() != null ? brand.getReviewSources().size() : 0)")
    BrandResponse toBrandResponse(Brand brand);
}
