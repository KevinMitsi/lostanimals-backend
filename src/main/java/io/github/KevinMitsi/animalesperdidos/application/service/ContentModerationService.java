package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ContentModerationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ModerationRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.ConversationReport;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class ContentModerationService implements ContentModerationUseCase {
    private final ModerationRepository moderation; private final Clock clock;
    public ContentModerationService(ModerationRepository moderation, Clock clock) {
        this.moderation = moderation; this.clock = clock;
    }
    @Override public CompletionStage<List<View>> pendingReports() {
        return moderation.pendingConversationReports(100).thenApply(values -> values.stream().map(this::view).toList());
    }
    @Override public CompletionStage<Void> decide(UUID moderatorId, UUID reportId, boolean resolved) {
        return moderation.findConversationReport(reportId).thenCompose(optional -> {
            if (optional.isEmpty()) return CompletableFuture.failedFuture(new ResourceNotFound("Conversation report"));
            try { return moderation.decideConversationReport(optional.get().decide(moderatorId, resolved, clock.instant()))
                    .thenApply(ignored -> null); }
            catch (IllegalStateException error) { return CompletableFuture.failedFuture(new BusinessRuleViolation(error.getMessage())); }
        });
    }
    private View view(ConversationReport value) { return new View(value.id(),value.conversationId(),value.reporterId(),
            value.reason(),value.details(),value.status(),value.createdAt()); }
}
