package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public record SightingResponse(@NotNull UUID id, @NotNull SpeciesDto species,
                               @NotBlank String description, @NotNull Instant observedAt,
                               @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
                               @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
                               @NotNull UUID neighborhoodId, @NotNull SightingStatusDto status,
                               @NotEmpty List<@Valid SightingImageResponse> images,
                               @NotNull Instant createdAt, @NotNull Instant updatedAt,
                               @PositiveOrZero long version) { }
