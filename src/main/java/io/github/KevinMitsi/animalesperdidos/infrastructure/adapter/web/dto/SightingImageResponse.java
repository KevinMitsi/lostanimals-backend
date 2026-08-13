package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SightingImageResponse(@NotNull UUID id, @NotBlank String url, boolean primary,
                                    @PositiveOrZero int sortOrder) { }
