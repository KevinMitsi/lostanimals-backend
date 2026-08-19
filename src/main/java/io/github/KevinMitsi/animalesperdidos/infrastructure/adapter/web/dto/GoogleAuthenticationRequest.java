package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthenticationRequest(
        @NotBlank @Size(max = 4096)
        @Schema(description = "Google Identity Services ID credential") String credential,
        @Schema(description = "Required only when this creates a new account") boolean acceptsDataProcessing) {
}
