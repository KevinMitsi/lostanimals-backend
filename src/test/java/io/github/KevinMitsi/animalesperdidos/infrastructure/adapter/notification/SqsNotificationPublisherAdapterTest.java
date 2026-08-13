package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.time.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsNotificationPublisherAdapterTest {
    @Mock SqsAsyncClient sqs; @Captor ArgumentCaptor<SendMessageRequest> request;
    @Test void verificationBecomesFifoSqsEventWithoutCallingSes(){
        AwsNotificationProperties properties=new AwsNotificationProperties();
        properties.setQueueUrl("https://sqs.us-east-1.amazonaws.com/123/notifications.fifo");
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("message").build()));
        var adapter=new SqsNotificationPublisherAdapter(sqs,new ObjectMapper().findAndRegisterModules(),properties,
                Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"),ZoneOffset.UTC));
        adapter.sendEmailVerification("ana@example.com","Ana","secret-token").toCompletableFuture().join();
        verify(sqs).sendMessage(request.capture());
        assertEquals("notifications",request.getValue().messageGroupId());
        assertTrue(request.getValue().messageBody().contains("EMAIL_VERIFICATION"));
        assertTrue(request.getValue().messageBody().contains("secret-token"));
    }
}
