package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.ConversationReport;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ContentModerationUseCase {
    CompletionStage<List<View>> pendingReports();
    CompletionStage<Void> decide(UUID moderatorId, UUID reportId, boolean resolved);
    record View(UUID id, UUID conversationId, UUID reporterId, String reason, String details,
                ConversationReport.Status status, Instant createdAt) { }
}
