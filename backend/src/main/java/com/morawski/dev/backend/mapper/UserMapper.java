package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.user.UserProfileResponse;
import com.morawski.dev.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for User entity to DTO conversions.
 * Extends BaseMapper for automatic Instant <-> ZonedDateTime conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper extends BaseMapper {

    /**
     * Convert User entity to UserProfileResponse DTO.
     * Instant fields are automatically converted to ZonedDateTime via BaseMapper.
     */
    @Mapping(source = "id", target = "userId")
    UserProfileResponse toUserProfileResponse(User user);
}
