package com.morawski.dev.backend.mapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Base mapper interface providing common conversion methods.
 * All entity mappers should extend this interface to inherit timestamp conversion utilities.
 *
 * Handles automatic conversion between:
 * - Instant (used in entities) <-> ZonedDateTime (used in DTOs)
 */
public interface BaseMapper {

    /**
     * Convert Instant to ZonedDateTime in UTC timezone.
     * Used automatically by MapStruct for all Instant -> ZonedDateTime mappings.
     *
     * @param instant The instant to convert
     * @return ZonedDateTime in UTC, or null if input is null
     */
    default ZonedDateTime instantToZonedDateTime(Instant instant) {
        return instant != null ? instant.atZone(ZoneId.of("UTC")) : null;
    }

    /**
     * Convert ZonedDateTime to Instant.
     * Used automatically by MapStruct for all ZonedDateTime -> Instant mappings.
     *
     * @param zonedDateTime The zoned date time to convert
     * @return Instant, or null if input is null
     */
    default Instant zonedDateTimeToInstant(ZonedDateTime zonedDateTime) {
        return zonedDateTime != null ? zonedDateTime.toInstant() : null;
    }
}
