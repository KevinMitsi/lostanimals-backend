package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
public record ModerationDecisionRequest(@NotNull Boolean approved,
                                        @NotBlank @Size(max=1000) String note) { }
