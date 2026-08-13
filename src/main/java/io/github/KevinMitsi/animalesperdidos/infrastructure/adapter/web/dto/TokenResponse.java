package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TokenResponse(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @NotBlank @Schema(description = "Opaque single-use rotating refresh token", requiredMode = Schema.RequiredMode.REQUIRED) String refreshToken,
        @NotBlank @Schema(example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED) String tokenType,
        @Positive @Schema(example = "3600", requiredMode = Schema.RequiredMode.REQUIRED) long expiresInSeconds,
        @Positive @Schema(example = "2592000", requiredMode = Schema.RequiredMode.REQUIRED) long refreshExpiresInSeconds) {
}
