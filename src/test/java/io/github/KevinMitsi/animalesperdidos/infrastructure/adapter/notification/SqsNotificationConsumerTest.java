package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.Event;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.Type;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsNotificationConsumerTest {
    @Mock SqsAsyncClient sqs;
    @Mock AwsNotificationDispatcher dispatcher;

    private ObjectMapper objectMapper;
    private SqsNotificationConsumer consumer;
    private Event event;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        AwsNotificationProperties properties = new AwsNotificationProperties();
        properties.setQueueUrl("https://sqs.us-east-1.amazonaws.com/123/notifications");
        consumer = new SqsNotificationConsumer(sqs, objectMapper, dispatcher, properties);
        event = new Event(UUID.randomUUID(), Type.EMAIL_VERIFICATION, null, "ana@example.com",
                Map.of("token", "one-use-token"), Instant.parse("2026-08-13T12:00:00Z"));
        Message message = Message.builder().body(objectMapper.writeValueAsString(event))
                .receiptHandle("receipt").build();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(
                ReceiveMessageResponse.builder().messages(message).build()));
    }

    @Test
    void deletesMessageOnlyAfterSuccessfulDispatch() {
        when(dispatcher.dispatch(event)).thenReturn(CompletableFuture.completedFuture(null));
        when(sqs.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(
                DeleteMessageResponse.builder().build()));

        consumer.poll();

        ArgumentCaptor<DeleteMessageRequest> deletion = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqs).deleteMessage(deletion.capture());
        assertEquals("receipt", deletion.getValue().receiptHandle());
    }

    @Test
    void leavesMessageForSqsRetryWhenDispatchFails() {
        when(dispatcher.dispatch(event)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("SES unavailable")));

        consumer.poll();

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
