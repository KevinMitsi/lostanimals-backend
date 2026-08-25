package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials and bot challenge used to obtain a JWT")
@NoSqlInjection
public record LoginRequest(
        @NotBlank @Email @Size(max = 254) @Schema(example = "ana@example.com") String email,
        @NotBlank @Size(min = 1, max = 72) @Schema(format = "password") String password,
        @NotBlank @Size(max = 2048) @Schema(description = "Single-use Cloudflare Turnstile token") String turnstileToken
) {
}
