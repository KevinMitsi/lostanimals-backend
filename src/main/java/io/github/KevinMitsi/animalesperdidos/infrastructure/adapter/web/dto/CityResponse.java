package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CityResponse(@NotNull UUID id, @NotNull UUID departmentId,
                           @NotBlank @Size(max = 100) String name) { }
