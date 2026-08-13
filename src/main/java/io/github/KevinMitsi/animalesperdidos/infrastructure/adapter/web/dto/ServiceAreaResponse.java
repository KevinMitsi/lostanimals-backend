package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ServiceAreaResponse(@NotNull UUID cityId, @NotBlank String cityName,
        @NotNull UUID departmentId, @NotBlank String departmentName, boolean enabled) { }
