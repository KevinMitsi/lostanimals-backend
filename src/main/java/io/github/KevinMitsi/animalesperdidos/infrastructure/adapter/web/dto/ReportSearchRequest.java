package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Filters for lost-pet reports; latitude, longitude and radiusMeters must be sent together")
public record ReportSearchRequest(SpeciesDto species, UUID departmentId, UUID cityId, UUID neighborhoodId,
                                  ReportStatusDto status, Instant from, Instant to,
                                  @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                  @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                  @Positive @DecimalMin("100.0") @DecimalMax("50000.0") Double radiusMeters,
                                  @Size(max = 200) String cursor,
                                  @Min(1) @Max(50) int limit) {
    public ReportSearchRequest {
        if (limit == 0) limit = 20;
    }
}
