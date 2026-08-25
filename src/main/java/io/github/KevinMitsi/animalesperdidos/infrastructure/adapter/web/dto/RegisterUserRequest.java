package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Information required to create a citizen account")
@NoSqlInjection
public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 254)
        @Schema(example = "ana@example.com") String email,
        @NotBlank @Size(min = 12, max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must include upper-case, lower-case and numeric characters")
        @Schema(example = "ClaveMuySegura2026") String password,
        @NotBlank @Pattern(regexp = "^\\+57[3][0-9]{9}$", message = "must use Colombian format +573XXXXXXXXX")
        @Schema(example = "+573001234567") String phone,
        @NotBlank @Pattern(regexp = "^[0-9]{6,10}$", message = "must contain 6 to 10 digits")
        @Schema(example = "1094912345") String documentNumber,
        @NotBlank @Size(min = 2, max = 100)
        @Schema(example = "Ana García") String displayName,
        @AssertTrue(message = "data processing consent is required")
        @Schema(example = "true") boolean acceptsDataProcessing,
        @NotBlank @Size(max = 2048)
        @Schema(description = "Single-use Cloudflare Turnstile token") String turnstileToken
) {
}
