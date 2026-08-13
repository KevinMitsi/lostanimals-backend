package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record Participant(UUID userId, Instant joinedAt, Instant leftAt) {
    public Participant { Objects.requireNonNull(userId); Objects.requireNonNull(joinedAt); }
    public boolean active() { return leftAt == null; }
}
