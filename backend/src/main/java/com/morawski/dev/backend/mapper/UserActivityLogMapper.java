package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.activity.UserActivityResponse;
import com.morawski.dev.backend.entity.UserActivityLog;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Mapper for UserActivityLog entity to DTOs.
 */
@Component
public class UserActivityLogMapper implements BaseMapper {

    /**
     * Convert UserActivityLog entity to UserActivityResponse DTO.
     *
     * @param entity UserActivityLog entity
     * @return UserActivityResponse DTO
     */
    public UserActivityResponse toUserActivityResponse(UserActivityLog entity) {
        if (entity == null) {
            return null;
        }

        return UserActivityResponse.builder()
            .activityId(entity.getId())
            .activityType(entity.getActivityType())
            .occurredAt(entity.getOccurredAt().atZone(ZoneId.of("UTC")))
            .metadata(entity.getMetadata())
            .build();
    }
}
