package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

@Schema(description = "Lost-pet report metadata with DIVIPOLA codes and a free-text neighborhood")
@NoSqlInjection
public record CreateLostPetReportRequest(
        @NotBlank @Size(max = 80) @Schema(example = "Luna") String petName,
        @NotNull @Schema(example = "DOG") SpeciesDto species,
        @NotBlank @Size(max = 2000) @Schema(example = "Lleva collar rojo") String description,
        @NotNull @PastOrPresent Instant disappearedAt,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @NotBlank @Pattern(regexp = "^[0-9]{2}$") String departmentCode,
        @NotBlank @Pattern(regexp = "^[0-9]{5}$") String municipalityCode,
        @NotBlank @Size(max = 120) String neighborhood,
        @NotNull @Size(min = 1, max = 5) List<@NotBlank @Size(max = 1024) String> imageKeys
) {
    public CreateLostPetReportRequest { imageKeys = imageKeys == null ? null : List.copyOf(imageKeys); }
}
