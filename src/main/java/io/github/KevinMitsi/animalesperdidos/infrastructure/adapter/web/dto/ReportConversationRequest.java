package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
public record ReportConversationRequest(@NotBlank @Size(max=40) String reason,
                                        @NotBlank @Size(max=1000) String details) { }
