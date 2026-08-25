package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ConversationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.MessageEventPublisher;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.websocket.ConversationWebSocketHandler;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.websocket.SpringMessageEventPublisher;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
public class WebSocketConfiguration {
    @Bean
    ExecutorSubscribableChannel conversationMessageChannel() {
        return new ExecutorSubscribableChannel();
    }

    @Bean
    MessageEventPublisher messageEventPublisher(ExecutorSubscribableChannel conversationMessageChannel) {
        return new SpringMessageEventPublisher(conversationMessageChannel);
    }

    @Bean
    ConversationWebSocketHandler conversationWebSocketHandler(ConversationUseCase conversations,
            ObjectMapper objectMapper, Validator validator, WebCorsProperties cors,
            ExecutorSubscribableChannel conversationMessageChannel) {
        ConversationWebSocketHandler handler = new ConversationWebSocketHandler(
                conversations, objectMapper, validator, cors);
        conversationMessageChannel.subscribe(handler);
        return handler;
    }

    @Bean
    HandlerMapping webSocketHandlerMapping(ConversationWebSocketHandler handler) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(Map.of("/ws/conversations/{conversationId}", handler));
        return mapping;
    }

    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
