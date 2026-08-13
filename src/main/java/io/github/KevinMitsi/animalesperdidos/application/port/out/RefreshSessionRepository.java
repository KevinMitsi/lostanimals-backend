package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface RefreshSessionRepository {
    CompletionStage<Void> save(RefreshSession session);
    CompletionStage<Optional<RefreshSession>> rotate(String currentHash, UUID replacementId,
                                                      String replacementHash, Instant replacementExpiresAt,
                                                      Instant now);
    CompletionStage<Boolean> revoke(String tokenHash, Instant now);
    CompletionStage<Void> revokeAllByUser(UUID userId, Instant now);
}
