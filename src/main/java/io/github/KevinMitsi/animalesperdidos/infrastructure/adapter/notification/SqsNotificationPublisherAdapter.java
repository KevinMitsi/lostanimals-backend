package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="true")
public class SqsNotificationPublisherAdapter implements NotificationPort,AccountNotificationPort,NotificationEventPublisher {
    private final SqsAsyncClient sqs; private final ObjectMapper objectMapper;
    private final AwsNotificationProperties properties; private final Clock clock;

    @Override public CompletionStage<Void> reportCreated(LostPetReport report){
        return publish(new Event(UUID.randomUUID(),Type.LOST_PET_REPORT_CREATED,report.ownerId(),null,
                Map.of("reportId",report.id().toString(),"petName",report.petName()),clock.instant()));
    }
    @Override public CompletionStage<Void> sendEmailVerification(String email,String displayName,String rawToken){
        return publish(new Event(UUID.randomUUID(),Type.EMAIL_VERIFICATION,null,email,
                Map.of("displayName",displayName,"token",rawToken),clock.instant()));
    }
    @Override public CompletionStage<Void> sendPasswordReset(String email,String displayName,String rawToken){
        return publish(new Event(UUID.randomUUID(),Type.PASSWORD_RESET,null,email,
                Map.of("displayName",displayName,"token",rawToken),clock.instant()));
    }
    @Override public CompletionStage<Void> publish(Event event){
        validate();
        try {
            SendMessageRequest.Builder request=SendMessageRequest.builder().queueUrl(properties.getQueueUrl())
                    .messageBody(objectMapper.writeValueAsString(event));
            if(properties.getQueueUrl().endsWith(".fifo")) request.messageGroupId("notifications")
                    .messageDeduplicationId(event.id().toString());
            return sqs.sendMessage(request.build()).thenApply(ignored->null);
        } catch(JsonProcessingException error){return CompletableFuture.failedFuture(error);}
    }
    private void validate(){if(properties.getQueueUrl()==null||properties.getQueueUrl().isBlank())
        throw new IllegalStateException("AWS_NOTIFICATION_QUEUE_URL is required");}
}
