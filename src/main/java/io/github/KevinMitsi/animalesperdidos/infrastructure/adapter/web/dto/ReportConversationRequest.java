package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
@NoSqlInjection
public record ReportConversationRequest(@NotBlank @Size(max=40) String reason,
                                        @NotBlank @Size(max=1000) String details) { }
