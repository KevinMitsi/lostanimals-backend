package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.NotNull;
@NoSqlInjection
public record ReportDecisionRequest(@NotNull Boolean resolved) { }
