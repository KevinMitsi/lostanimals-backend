package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.Conversation;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ConversationUseCase {
    CompletionStage<List<View>> list(UUID actorId);
    CompletionStage<Void> verifyAccess(UUID actorId, UUID conversationId);
    CompletionStage<MessagePage> messages(UUID actorId, UUID conversationId, String after, int limit);
    CompletionStage<UUID> send(UUID actorId, UUID conversationId, String content);
    CompletionStage<Void> close(UUID actorId, UUID conversationId);
    CompletionStage<Void> block(UUID actorId, UUID conversationId);
    CompletionStage<UUID> report(UUID actorId, UUID conversationId, String reason, String details);
    record View(UUID id, Conversation.Status status, List<ParticipantView> participants,
                Instant createdAt, Instant closedAt) { }
    record ParticipantView(UUID userId, String displayName) { }
    record MessageView(UUID id, UUID senderId, String content, Instant createdAt) { }
    record MessagePage(List<MessageView> items, String nextAfter) { }
}
