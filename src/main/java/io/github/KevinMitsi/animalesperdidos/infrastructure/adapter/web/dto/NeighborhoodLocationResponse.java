package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NeighborhoodLocationResponse(@NotNull UUID departmentId, @NotBlank String departmentName,
                                           @NotNull UUID cityId, @NotBlank String cityName,
                                           @NotNull UUID neighborhoodId, @NotBlank String neighborhoodName) { }
