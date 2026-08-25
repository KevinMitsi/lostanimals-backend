package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ConversationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class ConversationService implements ConversationUseCase {
    private final ContactRepository contacts; private final UserRepository users;
    private final MessageEventPublisher messageEvents; private final Clock clock;
    public ConversationService(ContactRepository contacts, UserRepository users,
                               MessageEventPublisher messageEvents, Clock clock) {
        this.contacts = contacts; this.users = users; this.messageEvents = messageEvents; this.clock = clock;
    }
    @Override public CompletionStage<List<View>> list(UUID actorId) {
        return contacts.conversationsFor(actorId).thenCompose(values -> sequence(values.stream().map(this::view).toList()));
    }
    @Override public CompletionStage<Void> verifyAccess(UUID actorId, UUID conversationId) {
        return conversation(actorId, conversationId).thenApply(ignored -> null);
    }
    @Override public CompletionStage<MessagePage> messages(UUID actorId, UUID conversationId, String after, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        SearchCriteriaPolicy.Cursor cursor = SearchCriteriaPolicy.decode(after);
        return conversation(actorId, conversationId).thenCompose(ignored -> contacts.messages(conversationId,
                cursor.createdAt(), cursor.id(), safeLimit)).thenApply(values -> {
            String checkpoint = values.isEmpty() ? after
                    : SearchCriteriaPolicy.encode(values.getLast().createdAt(), values.getLast().id());
            return new MessagePage(values.stream().map(this::message).toList(), checkpoint);
        });
    }
    @Override public CompletionStage<UUID> send(UUID actorId, UUID conversationId, String content) {
        return conversation(actorId, conversationId).thenCompose(value -> {
            if (value.status() != Conversation.Status.OPEN) return failed(new BusinessRuleViolation("Conversation is closed"));
            UUID other = other(value, actorId);
            return contacts.blockedBetween(actorId, other).thenCompose(blocked -> {
                if (blocked) return failed(new ForbiddenOperation());
                Message message = new Message(UUID.randomUUID(), conversationId, actorId, content, clock.instant());
                return contacts.saveMessage(message)
                        .thenCompose(saved -> messageEvents.publish(saved).thenApply(ignored -> saved.id()));
            });
        });
    }
    @Override public CompletionStage<Void> close(UUID actorId, UUID conversationId) {
        return conversation(actorId, conversationId).thenCompose(value -> {
            try { return contacts.updateConversation(value.close(actorId, clock.instant())).thenApply(ignored -> null); }
            catch (IllegalStateException error) { return failed(new BusinessRuleViolation(error.getMessage())); }
        });
    }
    @Override public CompletionStage<Void> block(UUID actorId, UUID conversationId) {
        return conversation(actorId, conversationId).thenCompose(value -> {
            UUID blocked = other(value, actorId);
            Conversation closed = value.status() == Conversation.Status.OPEN ? value.close(actorId, clock.instant()) : value;
            return contacts.saveBlock(new UserBlock(actorId, blocked, conversationId, clock.instant()), closed);
        });
    }
    @Override public CompletionStage<UUID> report(UUID actorId, UUID conversationId, String reason, String details) {
        return conversation(actorId, conversationId).thenCompose(ignored -> {
            ConversationReport report = ConversationReport.create(UUID.randomUUID(), conversationId, actorId,
                    reason, details, clock.instant());
            return contacts.saveReport(report).thenApply(ConversationReport::id);
        });
    }
    private CompletionStage<View> view(Conversation value) {
        return sequence(value.participants().stream().map(participant -> users.findById(participant.userId())
                .thenCompose(user -> user.<CompletionStage<ParticipantView>>map(found -> completed(
                        new ParticipantView(found.id(), found.displayName())))
                        .orElseGet(() -> failed(new ResourceNotFound("Conversation participant"))))).toList())
                .thenApply(participants -> new View(value.id(), value.status(), participants, value.createdAt(), value.closedAt()));
    }
    private CompletionStage<Conversation> conversation(UUID actor, UUID id) {
        return contacts.findConversation(id).thenCompose(value -> {
            if (value.isEmpty()) return failed(new ResourceNotFound("Conversation"));
            if (!value.get().hasParticipant(actor)) return failed(new ResourceNotFound("Conversation"));
            return completed(value.get());
        });
    }
    private static UUID other(Conversation conversation, UUID actor) {
        return conversation.participants().stream().map(Participant::userId).filter(id -> !id.equals(actor)).findFirst()
                .orElseThrow(() -> new ForbiddenOperation());
    }
    private MessageView message(Message value) { return new MessageView(value.id(), value.senderId(), value.content(), value.createdAt()); }
    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        CompletableFuture<?>[] futures = stages.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> stages.stream().map(s -> s.toCompletableFuture().join()).toList());
    }
    private static <T> CompletionStage<T> completed(T value) { return CompletableFuture.completedFuture(value); }
    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
