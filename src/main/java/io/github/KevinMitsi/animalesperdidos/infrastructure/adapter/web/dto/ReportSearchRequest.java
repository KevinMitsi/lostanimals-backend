package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReportSearchRequest(SpeciesDto species, UUID neighborhoodId, ReportStatusDto status,
                                  @Size(max = 200) String cursor,
                                  @Min(1) @Max(50) int limit) {
    public ReportSearchRequest {
        if (limit == 0) limit = 20;
    }
}
