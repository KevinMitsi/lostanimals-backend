package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ReunionModerationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class ReunionModerationService implements ReunionModerationUseCase {
    private final ModerationRepository moderation; private final LostPetReportRepository reports;
    private final UserRepository users; private final Clock clock;
    public ReunionModerationService(ModerationRepository moderation, LostPetReportRepository reports,
                                    UserRepository users, Clock clock) {
        this.moderation = moderation; this.reports = reports; this.users = users; this.clock = clock;
    }
    @Override public CompletionStage<UUID> request(UUID ownerId, UUID reportId, String note) {
        return report(reportId).thenCompose(report -> {
            if (!report.ownerId().equals(ownerId)) return failed(new ForbiddenOperation());
            if (report.status() != ReportStatus.LOST) return failed(new BusinessRuleViolation("Report is not active"));
            ReunionReview review = ReunionReview.request(UUID.randomUUID(), reportId, ownerId, note, clock.instant());
            return moderation.save(review).thenApply(ReunionReview::id);
        });
    }
    @Override public CompletionStage<List<View>> pending(UUID moderatorId) {
        return RoleGuard.require(users,moderatorId,UserRole.MODERATOR,UserRole.ADMIN)
                .thenCompose(ignored->moderation.pendingReviews(100))
                .thenCompose(values -> sequence(values.stream().map(this::view).toList()));
    }
    @Override public CompletionStage<Void> decide(UUID moderatorId, UUID reviewId, boolean approved, String note) {
        return RoleGuard.require(users,moderatorId,UserRole.MODERATOR,UserRole.ADMIN)
                .thenCompose(ignored->moderation.findReview(reviewId)).thenCompose(optional -> optional
                .<CompletionStage<ReunionReview>>map(ConversationServiceHelper::completed)
                .orElseGet(() -> failed(new ResourceNotFound("Reunion review"))))
                .thenCompose(review -> report(review.reportId()).thenCompose(report -> {
                    ReunionReview decided;
                    try { decided = review.decide(moderatorId, approved, note, clock.instant()); }
                    catch (IllegalStateException error) { return failed(new BusinessRuleViolation(error.getMessage())); }
                    LostPetReport resulting = approved ? report.changeStatus(ReportStatus.REUNITED, clock.instant()) : report;
                    return moderation.decide(decided, resulting).thenApply(ignored -> null);
                }));
    }
    private CompletionStage<View> view(ReunionReview review) {
        return users.findById(review.requestedBy()).thenCompose(optional -> optional.<CompletionStage<View>>map(user ->
                ConversationServiceHelper.completed(new View(review.id(), review.reportId(), user.id(), user.displayName(),
                        user.phone(), review.requestNote(), review.status(), review.createdAt())))
                .orElseGet(() -> failed(new ResourceNotFound("Report owner"))));
    }
    private CompletionStage<LostPetReport> report(UUID id) { return reports.findById(id).thenCompose(optional -> optional
            .<CompletionStage<LostPetReport>>map(ConversationServiceHelper::completed)
            .orElseGet(() -> failed(new ResourceNotFound("Lost-pet report")))); }
    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        var futures = stages.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> stages.stream().map(s -> s.toCompletableFuture().join()).toList());
    }
    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
    private static final class ConversationServiceHelper {
        private static <T> CompletionStage<T> completed(T value) { return CompletableFuture.completedFuture(value); }
    }
}
