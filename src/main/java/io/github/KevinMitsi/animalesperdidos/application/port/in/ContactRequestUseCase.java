package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ContactRequestUseCase {
    CompletionStage<UUID> create(UUID actorId, Command command);
    CompletionStage<List<View>> received(UUID actorId);
    CompletionStage<List<View>> sent(UUID actorId);
    CompletionStage<UUID> accept(UUID actorId, UUID requestId);
    CompletionStage<Void> reject(UUID actorId, UUID requestId);
    CompletionStage<Void> cancel(UUID actorId, UUID requestId);
    record Command(PublicationType publicationType, UUID publicationId, String note) { }
    record View(UUID id, PublicationType publicationType, UUID publicationId, UUID requesterId,
                UUID recipientId, ContactRequest.Status status, String note, Instant createdAt, Instant answeredAt) { }
}
