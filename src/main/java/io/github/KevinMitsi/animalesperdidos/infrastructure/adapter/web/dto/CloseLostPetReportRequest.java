package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CloseLostPetReportRequest(
        @NotNull @Schema(description = "REUNITED when the animal returned home, CLOSED for another reason, or LOST to reopen within 30 days",
                allowableValues = {"LOST", "REUNITED", "CLOSED"}) ReportStatusDto status) { }
