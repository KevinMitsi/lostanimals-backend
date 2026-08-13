package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

@Schema(description = "Information required to publish an animal sighting")
public record CreateSightingRequest(
        @NotNull SpeciesDto species,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @PastOrPresent Instant observedAt,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @NotNull UUID neighborhoodId,
        @NotNull @Size(min = 1, max = 5) List<@NotBlank @Size(max = 1024) String> imageKeys,
        @Schema(description = "Explicitly publish even when a nearby sighting was detected")
        boolean confirmPossibleDuplicate) {
    public CreateSightingRequest { imageKeys = imageKeys == null ? null : List.copyOf(imageKeys); }
}
