package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.Event;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="true")
public class SqsNotificationConsumer {
    private static final Logger log=LoggerFactory.getLogger(SqsNotificationConsumer.class);
    private final SqsAsyncClient sqs; private final ObjectMapper objectMapper;
    private final AwsNotificationDispatcher dispatcher; private final AwsNotificationProperties properties;
    private final AtomicBoolean polling=new AtomicBoolean();

    @Scheduled(fixedDelayString="${app.notifications.aws.poll-delay-ms:1000}")
    public void poll(){
        if(!polling.compareAndSet(false,true))return;
        ReceiveMessageRequest request=ReceiveMessageRequest.builder().queueUrl(properties.getQueueUrl())
                .maxNumberOfMessages(properties.getMaxMessages()).waitTimeSeconds(properties.getWaitTimeSeconds())
                .visibilityTimeout(properties.getVisibilityTimeoutSeconds()).build();
        sqs.receiveMessage(request).thenCompose(response->CompletableFuture.allOf(response.messages().stream()
                .map(this::process).toArray(CompletableFuture[]::new)))
                .whenComplete((ignored,error)->{polling.set(false);if(error!=null)log.error("SQS notification polling failed",error);});
    }
    private CompletableFuture<Void> process(software.amazon.awssdk.services.sqs.model.Message message){
        try{
            Event event=objectMapper.readValue(message.body(),Event.class);
            return dispatcher.dispatch(event).thenCompose(ignored->sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(properties.getQueueUrl()).receiptHandle(message.receiptHandle()).build()))
                    .thenApply(ignored->(Void)null).toCompletableFuture();
        }catch(Exception error){
            log.error("Invalid SQS notification event; message will follow redrive policy",error);
            return CompletableFuture.failedFuture(error);
        }
    }
}
