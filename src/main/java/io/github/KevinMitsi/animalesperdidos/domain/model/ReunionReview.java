package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record ReunionReview(UUID id, UUID reportId, UUID requestedBy, String requestNote, Status status,
                            Instant createdAt, UUID reviewedBy, String reviewNote, Instant reviewedAt) {
    public enum Status { PENDING, APPROVED, REJECTED }
    public ReunionReview {
        Objects.requireNonNull(id); Objects.requireNonNull(reportId); Objects.requireNonNull(requestedBy);
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
        requestNote = ContactRequest.text(requestNote, 500, "requestNote");
        if (reviewNote != null && reviewNote.length() > 1000) throw new IllegalArgumentException("reviewNote is too long");
    }
    public static ReunionReview request(UUID id, UUID report, UUID owner, String note, Instant now) {
        return new ReunionReview(id, report, owner, note, Status.PENDING, now, null, null, null);
    }
    public ReunionReview decide(UUID moderator, boolean approved, String note, Instant now) {
        if (status != Status.PENDING) throw new IllegalStateException("Review already decided");
        return new ReunionReview(id, reportId, requestedBy, requestNote,
                approved ? Status.APPROVED : Status.REJECTED, createdAt, moderator,
                ContactRequest.text(note, 1000, "reviewNote"), now);
    }
}
