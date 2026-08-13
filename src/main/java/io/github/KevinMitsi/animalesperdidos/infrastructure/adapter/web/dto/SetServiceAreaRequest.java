package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.NotNull;
public record SetServiceAreaRequest(@NotNull Boolean enabled) { }
