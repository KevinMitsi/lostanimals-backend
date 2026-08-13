package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.NotNull;
public record ReportDecisionRequest(@NotNull Boolean resolved) { }
