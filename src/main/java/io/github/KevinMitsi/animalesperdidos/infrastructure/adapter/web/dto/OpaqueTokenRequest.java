package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NoSqlInjection
public record OpaqueTokenRequest(
        @NotBlank @Size(min = 32, max = 512) @Schema(description = "Opaque one-time token") String token) { }
