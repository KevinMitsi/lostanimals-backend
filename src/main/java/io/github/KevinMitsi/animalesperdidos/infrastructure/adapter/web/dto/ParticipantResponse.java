package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ParticipantResponse(@NotNull UUID userId, @NotBlank @Size(max=100) String displayName) { }
