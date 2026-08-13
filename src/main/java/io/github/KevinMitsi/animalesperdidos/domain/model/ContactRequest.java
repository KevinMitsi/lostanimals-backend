package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record ContactRequest(UUID id, PublicationRef publication, UUID requesterId, UUID recipientId,
                             Status status, String note, Instant createdAt, Instant answeredAt, long version) {
    public enum Status { PENDING, ACCEPTED, REJECTED, CANCELED }
    public ContactRequest {
        Objects.requireNonNull(id); Objects.requireNonNull(publication); Objects.requireNonNull(requesterId);
        Objects.requireNonNull(recipientId); Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
        note = text(note, 500, "note");
        if (requesterId.equals(recipientId)) throw new IllegalArgumentException("Cannot contact your own publication");
        if ((status == Status.PENDING || status == Status.CANCELED) && answeredAt != null)
            throw new IllegalArgumentException("Pending/canceled request cannot have answer time");
        if ((status == Status.ACCEPTED || status == Status.REJECTED) && answeredAt == null)
            throw new IllegalArgumentException("Answered request requires answer time");
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }
    public static ContactRequest create(UUID id, PublicationRef publication, UUID requester, UUID recipient,
                                        String note, Instant now) {
        return new ContactRequest(id, publication, requester, recipient, Status.PENDING, note, now, null, 0);
    }
    public ContactRequest accept(UUID actor, Instant now) { return answer(actor, Status.ACCEPTED, now); }
    public ContactRequest reject(UUID actor, Instant now) { return answer(actor, Status.REJECTED, now); }
    public ContactRequest cancel(UUID actor) {
        if (!requesterId.equals(actor)) throw new IllegalStateException("Only requester can cancel");
        requirePending(); return new ContactRequest(id, publication, requesterId, recipientId, Status.CANCELED,
                note, createdAt, null, version);
    }
    private ContactRequest answer(UUID actor, Status result, Instant now) {
        if (!recipientId.equals(actor)) throw new IllegalStateException("Only recipient can answer");
        requirePending(); return new ContactRequest(id, publication, requesterId, recipientId, result,
                note, createdAt, Objects.requireNonNull(now), version);
    }
    private void requirePending() { if (status != Status.PENDING) throw new IllegalStateException("Request is already answered"); }
    static String text(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        value = value.trim(); if (value.length() > max) throw new IllegalArgumentException(field + " is too long");
        return value;
    }
}
