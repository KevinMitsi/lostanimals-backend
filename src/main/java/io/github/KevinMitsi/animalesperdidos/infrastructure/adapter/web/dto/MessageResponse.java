package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record MessageResponse(@NotNull UUID id, @NotNull UUID senderId,
                              @NotBlank @Size(max=2000) String content, @NotNull Instant createdAt) { }
