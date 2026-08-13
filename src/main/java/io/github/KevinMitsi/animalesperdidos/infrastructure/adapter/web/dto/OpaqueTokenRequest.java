package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpaqueTokenRequest(
        @NotBlank @Size(min = 32, max = 512) @Schema(description = "Opaque one-time token") String token) { }
