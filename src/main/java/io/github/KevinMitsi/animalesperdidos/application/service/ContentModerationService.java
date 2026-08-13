package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ContentModerationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ModerationRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.ConversationReport;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class ContentModerationService implements ContentModerationUseCase {
    private final ModerationRepository moderation; private final UserRepository users; private final Clock clock;
    public ContentModerationService(ModerationRepository moderation, UserRepository users, Clock clock) {
        this.moderation = moderation; this.users = users; this.clock = clock;
    }
    @Override public CompletionStage<List<View>> pendingReports(UUID moderatorId) {
        return RoleGuard.require(users,moderatorId,UserRole.MODERATOR,UserRole.ADMIN)
                .thenCompose(ignored->moderation.pendingConversationReports(100))
                .thenApply(values -> values.stream().map(this::view).toList());
    }
    @Override public CompletionStage<Void> decide(UUID moderatorId, UUID reportId, boolean resolved) {
        return RoleGuard.require(users,moderatorId,UserRole.MODERATOR,UserRole.ADMIN)
                .thenCompose(ignored->moderation.findConversationReport(reportId)).thenCompose(optional -> {
            if (optional.isEmpty()) return CompletableFuture.failedFuture(new ResourceNotFound("Conversation report"));
            try { return moderation.decideConversationReport(optional.get().decide(moderatorId, resolved, clock.instant()))
                    .thenApply(ignored -> null); }
            catch (IllegalStateException error) { return CompletableFuture.failedFuture(new BusinessRuleViolation(error.getMessage())); }
        });
    }
    private View view(ConversationReport value) { return new View(value.id(),value.conversationId(),value.reporterId(),
            value.reason(),value.details(),value.status(),value.createdAt()); }
}
