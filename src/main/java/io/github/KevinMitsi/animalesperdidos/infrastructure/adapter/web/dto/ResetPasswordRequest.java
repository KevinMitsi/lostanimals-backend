package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@NoSqlInjection
public record ResetPasswordRequest(
        @NotBlank @Size(min = 32, max = 512) String token,
        @NotBlank @Size(min = 12, max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must include upper-case, lower-case and numeric characters")
        @Schema(format = "password") String newPassword) { }
