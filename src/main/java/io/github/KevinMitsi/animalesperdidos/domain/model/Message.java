package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.*;

public record Message(UUID id, UUID conversationId, UUID senderId, String content, Instant createdAt) {
    public Message {
        Objects.requireNonNull(id); Objects.requireNonNull(conversationId); Objects.requireNonNull(senderId);
        Objects.requireNonNull(createdAt); content = ContactRequest.text(content, 2000, "content");
    }
}
