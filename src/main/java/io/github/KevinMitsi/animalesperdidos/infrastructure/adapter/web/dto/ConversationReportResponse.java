package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record ConversationReportResponse(@NotNull UUID id, @NotNull UUID conversationId, @NotNull UUID reporterId,
        @NotBlank String reason, @NotBlank String details, @NotNull ConversationReportStatusDto status,
        @NotNull Instant createdAt) { }
