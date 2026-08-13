package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record EditLostPetReportRequest(
        @NotBlank @Size(max = 80) String petName,
        @NotNull SpeciesDto species,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @PastOrPresent Instant disappearedAt,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @NotNull UUID neighborhoodId) { }
