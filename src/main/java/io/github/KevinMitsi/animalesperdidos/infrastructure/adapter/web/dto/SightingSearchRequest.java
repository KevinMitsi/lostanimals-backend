package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Filters for sightings; latitude, longitude and radiusMeters must be sent together")
@NoSqlInjection
public record SightingSearchRequest(SpeciesDto species, UUID departmentId, UUID cityId, UUID neighborhoodId,
                                    SightingStatusDto status, Instant from, Instant to,
                                    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                    @Positive @DecimalMin("100.0") @DecimalMax("50000.0") Double radiusMeters,
                                    @Size(max = 200) String cursor, @Min(1) @Max(50) int limit) {
    public SightingSearchRequest { if (limit == 0) limit = 20; }
}
