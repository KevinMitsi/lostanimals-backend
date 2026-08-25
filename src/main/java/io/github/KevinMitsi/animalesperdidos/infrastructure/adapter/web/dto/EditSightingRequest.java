package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

@NoSqlInjection
public record EditSightingRequest(@NotNull SpeciesDto species,
                                  @NotBlank @Size(max = 2000) String description,
                                  @NotNull @PastOrPresent Instant observedAt,
                                  @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
                                  @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
                                  @NotNull UUID neighborhoodId) { }
