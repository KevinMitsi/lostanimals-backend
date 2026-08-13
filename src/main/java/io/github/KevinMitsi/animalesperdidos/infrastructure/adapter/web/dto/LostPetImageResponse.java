package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record LostPetImageResponse(@NotNull UUID id, @NotBlank @Schema(format = "uri") String url,
                                   boolean primary, @PositiveOrZero int sortOrder) { }
