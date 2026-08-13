package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefreshSession(UUID id, UUID userId, String tokenHash, Instant expiresAt,
                             Instant revokedAt, UUID replacedById, Instant createdAt) {
    public RefreshSession {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(createdAt);
    }

    public boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
