package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record IdResponse(@NotNull UUID id) { }
