package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.ReunionReview;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.KevinMitsi.animalesperdidos.domain.model.ConversationReport;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ModerationRepository {
    CompletionStage<ReunionReview> save(ReunionReview review);
    CompletionStage<Optional<ReunionReview>> findReview(UUID id);
    CompletionStage<List<ReunionReview>> pendingReviews(int limit);
    CompletionStage<ReunionReview> decide(ReunionReview review, LostPetReport report);
    CompletionStage<Optional<ConversationReport>> findConversationReport(UUID id);
    CompletionStage<List<ConversationReport>> pendingConversationReports(int limit);
    CompletionStage<ConversationReport> decideConversationReport(ConversationReport report);
}
