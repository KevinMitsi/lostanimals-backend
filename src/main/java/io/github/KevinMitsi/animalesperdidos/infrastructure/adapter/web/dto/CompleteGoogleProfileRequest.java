package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@NoSqlInjection
public record CompleteGoogleProfileRequest(
        @NotBlank @Pattern(regexp = "^\\+57[3][0-9]{9}$", message = "must use Colombian format +573XXXXXXXXX")
        @Schema(example = "+573001234567") String phone,
        @NotBlank @Pattern(regexp = "^[0-9]{6,10}$", message = "must contain 6 to 10 digits")
        @Schema(example = "1094912345") String documentNumber) {
}
