package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ContactRepository {
    CompletionStage<ContactRequest> saveRequest(ContactRequest request);
    CompletionStage<Optional<ContactRequest>> findRequest(UUID id);
    CompletionStage<ContactRequest> updateRequest(ContactRequest request);
    CompletionStage<Conversation> accept(ContactRequest request, Conversation conversation);
    CompletionStage<List<ContactRequest>> requestsFor(UUID userId, boolean received);
    CompletionStage<Optional<Conversation>> findConversation(UUID id);
    CompletionStage<List<Conversation>> conversationsFor(UUID userId);
    CompletionStage<Conversation> updateConversation(Conversation conversation);
    CompletionStage<Message> saveMessage(Message message);
    CompletionStage<List<Message>> messages(UUID conversationId, Instant afterCreatedAt, UUID afterId, int limit);
    CompletionStage<Boolean> blockedBetween(UUID first, UUID second);
    CompletionStage<Void> saveBlock(UserBlock block, Conversation closedConversation);
    CompletionStage<ConversationReport> saveReport(ConversationReport report);
}
