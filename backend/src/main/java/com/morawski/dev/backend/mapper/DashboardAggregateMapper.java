package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.dashboard.SentimentDistributionResponse;
import com.morawski.dev.backend.entity.DashboardAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * MapStruct mapper for DashboardAggregate entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DashboardAggregateMapper extends BaseMapper {

    /**
     * Convert DashboardAggregate entity to SentimentDistributionResponse DTO.
     * Maps count fields and calculates percentage values as BigDecimal.
     */
    @Mapping(source = "positiveCount", target = "positive")
    @Mapping(source = "negativeCount", target = "negative")
    @Mapping(source = "neutralCount", target = "neutral")
    @Mapping(target = "positivePercentage", expression = "java(toBigDecimal(aggregate.getPositivePercentage()))")
    @Mapping(target = "negativePercentage", expression = "java(toBigDecimal(aggregate.getNegativePercentage()))")
    @Mapping(target = "neutralPercentage", expression = "java(toBigDecimal(aggregate.getNeutralPercentage()))")
    SentimentDistributionResponse toSentimentDistributionResponse(DashboardAggregate aggregate);

    /**
     * Convert double percentage to BigDecimal with 2 decimal places.
     */
    default BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
