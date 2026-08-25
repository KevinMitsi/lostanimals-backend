package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.websocket;

import io.github.KevinMitsi.animalesperdidos.application.port.out.MessageEventPublisher;
import io.github.KevinMitsi.animalesperdidos.domain.model.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class SpringMessageEventPublisher implements MessageEventPublisher {
    public static final String CONVERSATION_ID = "conversationId";

    private final MessageChannel channel;

    public SpringMessageEventPublisher(MessageChannel channel) {
        this.channel = channel;
    }

    @Override
    public CompletionStage<Void> publish(Message message) {
        channel.send(MessageBuilder.withPayload(message)
                .setHeader(CONVERSATION_ID, message.conversationId())
                .build());
        return CompletableFuture.completedFuture(null);
    }
}
