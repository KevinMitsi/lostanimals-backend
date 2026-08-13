package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLostPetReportResponse(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
}
