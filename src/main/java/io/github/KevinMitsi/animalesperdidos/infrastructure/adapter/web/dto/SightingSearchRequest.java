package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Filters for sightings; latitude, longitude and radiusMeters must be sent together")
@NoSqlInjection
public record SightingSearchRequest(SpeciesDto species,
                                    @Pattern(regexp = "^[0-9]{2}$") String departmentCode,
                                    @Pattern(regexp = "^[0-9]{5}$") String municipalityCode,
                                    @Size(max = 120) String neighborhood,
                                    SightingStatusDto status, Instant from, Instant to,
                                    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                    @Positive @DecimalMin("100.0") @DecimalMax("50000.0") Double radiusMeters,
                                    @Size(max = 200) String cursor, @Min(1) @Max(50) int limit) {
    public SightingSearchRequest { if (limit == 0) limit = 20; }
}
