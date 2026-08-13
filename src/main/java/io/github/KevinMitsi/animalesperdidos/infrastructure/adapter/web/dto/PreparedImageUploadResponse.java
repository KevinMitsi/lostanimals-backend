package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Map;

public record PreparedImageUploadResponse(
        @NotBlank String objectKey,
        @NotBlank @Schema(format = "uri") String uploadUrl,
        @NotBlank @Schema(example = "PUT") String method,
        @NotNull Map<@NotBlank String, @NotBlank String> requiredHeaders,
        @NotNull @Future Instant expiresAt) { }
