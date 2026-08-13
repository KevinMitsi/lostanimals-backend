package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccountToken(UUID id, UUID userId, Type type, String tokenHash,
                           Instant expiresAt, Instant consumedAt, Instant createdAt) {
    public AccountToken {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(createdAt);
        if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("Token expiry must be after creation");
    }

    public boolean usableAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public enum Type { EMAIL_VERIFICATION, PASSWORD_RESET }
}
