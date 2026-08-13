package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisteredUserResponse(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID userId,
        @NotNull @Email @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email) {
}
