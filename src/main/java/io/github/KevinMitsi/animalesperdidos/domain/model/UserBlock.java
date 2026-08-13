package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record UserBlock(UUID blockerId, UUID blockedId, UUID conversationId, Instant createdAt) {
    public UserBlock {
        Objects.requireNonNull(blockerId); Objects.requireNonNull(blockedId); Objects.requireNonNull(createdAt);
        if (blockerId.equals(blockedId)) throw new IllegalArgumentException("Cannot block yourself");
    }
}
