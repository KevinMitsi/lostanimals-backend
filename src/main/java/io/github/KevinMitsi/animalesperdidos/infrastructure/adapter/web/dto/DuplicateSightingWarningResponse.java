package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record DuplicateSightingWarningResponse(@NotNull UUID existingSightingId,
                                                @PositiveOrZero double distanceMeters,
                                                @NotNull Instant observedAt) { }
