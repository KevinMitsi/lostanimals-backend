package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ConversationUseCase;
import io.github.KevinMitsi.animalesperdidos.domain.model.Message;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.MessageResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.SendMessageRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.WebCorsProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class ConversationWebSocketHandler implements WebSocketHandler, MessageHandler {
    private static final int MAX_FRAME_LENGTH = 4_096;

    private final ConversationUseCase conversations;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Set<String> allowedOrigins;
    private final Map<UUID, Map<String, Sinks.Many<String>>> sessions = new ConcurrentHashMap<>();

    public ConversationWebSocketHandler(ConversationUseCase conversations, ObjectMapper objectMapper,
                                        Validator validator, WebCorsProperties cors) {
        this.conversations = conversations;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.allowedOrigins = Set.copyOf(cors.getAllowedOrigins());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        UUID conversationId;
        try {
            conversationId = conversationId(session.getHandshakeInfo().getUri());
        } catch (IllegalArgumentException error) {
            return session.close();
        }
        if (!originAllowed(session)) {
            return session.close();
        }
        return session.getHandshakeInfo().getPrincipal()
                .switchIfEmpty(Mono.error(new ForbiddenOperation()))
                .map(this::actorId)
                .flatMap(actorId -> Mono.fromCompletionStage(conversations.verifyAccess(actorId, conversationId))
                        .then(connected(session, actorId, conversationId)))
                .onErrorResume(ignored -> session.close());
    }

    private Mono<Void> connected(WebSocketSession session, UUID actorId, UUID conversationId) {
        Sinks.Many<String> outbound = Sinks.many().unicast()
                .onBackpressureBuffer(new ArrayBlockingQueue<>(256));
        sessions.computeIfAbsent(conversationId, ignored -> new ConcurrentHashMap<>()).put(session.getId(), outbound);

        Mono<Void> receive = session.receive()
                .concatMap(frame -> processFrame(actorId, conversationId, frame.getPayloadAsText(), outbound))
                .then();
        Mono<Void> send = session.send(outbound.asFlux().map(session::textMessage));

        return Mono.firstWithSignal(receive, send)
                .doFinally(ignored -> remove(conversationId, session.getId()));
    }

    private Mono<Void> processFrame(UUID actorId, UUID conversationId, String json, Sinks.Many<String> outbound) {
        if (json.length() > MAX_FRAME_LENGTH) {
            emitError(outbound, "PAYLOAD_TOO_LARGE", "El mensaje supera el tamaño permitido");
            return Mono.empty();
        }
        try {
            SendMessageRequest request = objectMapper.readValue(json, SendMessageRequest.class);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                emitError(outbound, "VALIDATION_ERROR", violations.iterator().next().getMessage());
                return Mono.empty();
            }
            return Mono.fromCompletionStage(conversations.send(actorId, conversationId, request.content()))
                    .then()
                    .onErrorResume(error -> {
                        emitApplicationError(outbound, error);
                        return Mono.empty();
                    });
        } catch (JsonProcessingException error) {
            emitError(outbound, "INVALID_PAYLOAD", "Se esperaba un JSON con el campo content");
            return Mono.empty();
        }
    }

    @Override
    public void handleMessage(org.springframework.messaging.Message<?> event) {
        if (!(event.getPayload() instanceof Message value)) {
            return;
        }
        Map<String, Sinks.Many<String>> recipients = sessions.get(value.conversationId());
        if (recipients == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(new MessageResponse(
                    value.id(), value.senderId(), value.content(), value.createdAt()));
            recipients.values().forEach(sink -> sink.tryEmitNext(json));
        } catch (JsonProcessingException ignored) {
            // A serialization failure must not roll back a message that was already persisted.
        }
    }

    private void emitApplicationError(Sinks.Many<String> outbound, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof BusinessRuleViolation) {
            emitError(outbound, "BUSINESS_RULE_VIOLATION", cause.getMessage());
        } else if (cause instanceof ForbiddenOperation) {
            emitError(outbound, "FORBIDDEN", "Operación no permitida");
        } else if (cause instanceof ResourceNotFound) {
            emitError(outbound, "NOT_FOUND", "Conversación no encontrada");
        } else {
            emitError(outbound, "INTERNAL_ERROR", "No fue posible enviar el mensaje");
        }
    }

    private void emitError(Sinks.Many<String> outbound, String code, String message) {
        try {
            outbound.tryEmitNext(objectMapper.writeValueAsString(new ErrorFrame(code, message)));
        } catch (JsonProcessingException ignored) {
            // Error frames only contain constant strings and cannot normally fail serialization.
        }
    }

    private boolean originAllowed(WebSocketSession session) {
        String origin = session.getHandshakeInfo().getHeaders().getOrigin();
        return origin == null || allowedOrigins.contains(origin);
    }

    private UUID actorId(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    private static UUID conversationId(URI uri) {
        String path = uri.getPath();
        int separator = path.lastIndexOf('/');
        return UUID.fromString(path.substring(separator + 1));
    }

    private void remove(UUID conversationId, String sessionId) {
        sessions.computeIfPresent(conversationId, (ignored, connected) -> {
            connected.remove(sessionId);
            return connected.isEmpty() ? null : connected;
        });
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record ErrorFrame(String code, String message) { }
}
