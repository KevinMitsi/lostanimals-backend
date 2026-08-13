package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record ConversationReport(UUID id, UUID conversationId, UUID reporterId, String reason, String details,
                                 Status status, Instant createdAt, UUID reviewedBy, Instant reviewedAt) {
    public enum Status { PENDING, RESOLVED, DISMISSED }
    public ConversationReport {
        Objects.requireNonNull(id); Objects.requireNonNull(conversationId); Objects.requireNonNull(reporterId);
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
        reason = ContactRequest.text(reason, 40, "reason"); details = ContactRequest.text(details, 1000, "details");
    }
    public static ConversationReport create(UUID id, UUID conversationId, UUID reporterId,
                                            String reason, String details, Instant now) {
        return new ConversationReport(id, conversationId, reporterId, reason, details,
                Status.PENDING, now, null, null);
    }
    public ConversationReport decide(UUID moderator, boolean resolved, Instant now) {
        if (status != Status.PENDING) throw new IllegalStateException("Report already reviewed");
        return new ConversationReport(id, conversationId, reporterId, reason, details,
                resolved ? Status.RESOLVED : Status.DISMISSED, createdAt, moderator, now);
    }
}
