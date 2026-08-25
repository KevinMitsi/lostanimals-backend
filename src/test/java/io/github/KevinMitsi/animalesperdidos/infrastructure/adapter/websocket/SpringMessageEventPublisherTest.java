package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.websocket;

import io.github.KevinMitsi.animalesperdidos.domain.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringMessageEventPublisherTest {
    @Test
    void publishesPersistedMessageWithConversationRoutingHeader() {
        ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
        AtomicReference<org.springframework.messaging.Message<?>> received = new AtomicReference<>();
        channel.subscribe(received::set);
        Message message = new Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Hola", Instant.parse("2026-08-25T21:30:00Z"));

        new SpringMessageEventPublisher(channel).publish(message).toCompletableFuture().join();

        assertEquals(message, received.get().getPayload());
        assertEquals(message.conversationId(),
                received.get().getHeaders().get(SpringMessageEventPublisher.CONVERSATION_ID));
    }
}
