package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SightingSearchRequest(SpeciesDto species, UUID neighborhoodId, SightingStatusDto status,
                                    @Size(max = 200) String cursor, @Min(1) @Max(50) int limit) {
    public SightingSearchRequest { if (limit == 0) limit = 20; }
}
