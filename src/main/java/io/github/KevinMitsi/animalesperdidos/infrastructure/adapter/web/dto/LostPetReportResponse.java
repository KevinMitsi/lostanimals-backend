package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LostPetReportResponse(
        @NotNull UUID id,
        @NotBlank String petName,
        @NotNull SpeciesDto species,
        @NotBlank String description,
        @NotNull Instant disappearedAt,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @NotNull UUID neighborhoodId,
        @NotNull ReportStatusDto status,
        @NotEmpty List<@Valid LostPetImageResponse> images,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt,
        @PositiveOrZero long version) { }
