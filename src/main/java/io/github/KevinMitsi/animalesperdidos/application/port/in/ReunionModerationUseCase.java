package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.ReunionReview;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ReunionModerationUseCase {
    CompletionStage<UUID> request(UUID ownerId, UUID reportId, String note);
    CompletionStage<List<View>> pending();
    CompletionStage<Void> decide(UUID moderatorId, UUID reviewId, boolean approved, String note);
    record View(UUID id, UUID reportId, UUID ownerId, String ownerName, String ownerPhone,
                String requestNote, ReunionReview.Status status, Instant createdAt) { }
}
