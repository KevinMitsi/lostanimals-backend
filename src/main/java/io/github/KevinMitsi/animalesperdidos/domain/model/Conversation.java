package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record Conversation(UUID id, UUID contactRequestId, Status status, List<Participant> participants,
                           Instant createdAt, Instant closedAt, long version) {
    public enum Status { OPEN, CLOSED }
    public Conversation {
        Objects.requireNonNull(id); Objects.requireNonNull(contactRequestId); Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt); participants = List.copyOf(participants);
        if (participants.size() != 2 || participants.stream().map(Participant::userId).distinct().count() != 2)
            throw new IllegalArgumentException("Conversation requires two distinct participants");
        if (status == Status.OPEN && closedAt != null) throw new IllegalArgumentException("Open conversation cannot be closed");
        if (status == Status.CLOSED && closedAt == null) throw new IllegalArgumentException("Closed conversation requires time");
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }
    public static Conversation open(UUID id, UUID requestId, UUID requester, UUID recipient, Instant now) {
        return new Conversation(id, requestId, Status.OPEN,
                List.of(new Participant(requester, now, null), new Participant(recipient, now, null)), now, null, 0);
    }
    public boolean hasParticipant(UUID userId) { return participants.stream().anyMatch(p -> p.userId().equals(userId)); }
    public Conversation close(UUID actor, Instant now) {
        if (!hasParticipant(actor)) throw new IllegalStateException("Not a participant");
        if (status == Status.CLOSED) throw new IllegalStateException("Conversation is already closed");
        return new Conversation(id, contactRequestId, Status.CLOSED, participants, createdAt, now, version);
    }
}
