package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CloseLostPetReportRequest(
        @NotNull @Schema(description = "CLOSED for an owner closure, or LOST to reopen within 30 days. REUNITED requires moderator verification.",
                allowableValues = {"LOST", "CLOSED"}) OwnerReportStatusDto status) { }
