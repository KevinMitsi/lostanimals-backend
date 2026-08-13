package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record NeighborhoodResponse(@NotNull UUID id, @NotNull UUID cityId,
                                   @NotBlank @Size(max = 120) String name) { }
