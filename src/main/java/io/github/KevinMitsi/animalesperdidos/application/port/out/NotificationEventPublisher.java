package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface NotificationEventPublisher {
    CompletionStage<Void> publish(Event event);

    record Event(UUID id, Type type, UUID recipientUserId, String recipientEmail,
                 Map<String,String> attributes, Instant occurredAt) {
        public Event {
            Objects.requireNonNull(id); Objects.requireNonNull(type); Objects.requireNonNull(occurredAt);
            if (recipientUserId == null && (recipientEmail == null || recipientEmail.isBlank()))
                throw new IllegalArgumentException("Notification recipient is required");
            attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        }
    }
    enum Type { EMAIL_VERIFICATION, PASSWORD_RESET, LOST_PET_REPORT_CREATED }
}
