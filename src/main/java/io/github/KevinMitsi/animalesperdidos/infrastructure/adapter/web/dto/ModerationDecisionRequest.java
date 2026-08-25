package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
@NoSqlInjection
public record ModerationDecisionRequest(@NotNull Boolean approved,
                                        @NotBlank @Size(max=1000) String note) { }
