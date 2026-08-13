package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.model.*;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsNotificationDispatcherTest {
    @Mock SesV2AsyncClient ses; @Mock SnsAsyncClient sns; @Mock UserRepository users;
    @Mock PushSubscriptionPort subscriptions; @Mock NotificationDeliveryLedger ledger;
    private AwsNotificationDispatcher dispatcher;
    @BeforeEach void setup(){AwsNotificationProperties p=new AwsNotificationProperties();p.setSenderEmail("no-reply@example.com");
        p.setFrontendBaseUrl("https://app.example.com");dispatcher=new AwsNotificationDispatcher(ses,sns,users,subscriptions,ledger,p);}
    @Test void verificationUsesSesAndNeverSns(){
        when(ledger.delivered(any(),eq("EMAIL"),eq("ana@example.com"))).thenReturn(done(false));
        when(ses.sendEmail(any(SendEmailRequest.class))).thenReturn(done(SendEmailResponse.builder().messageId("id").build()));
        when(ledger.mark(any(),eq("EMAIL"),eq("ana@example.com"))).thenReturn(done(null));
        dispatcher.dispatch(new Event(UUID.randomUUID(),Type.EMAIL_VERIFICATION,null,"ana@example.com",
                Map.of("displayName","Ana","token","token"),Instant.now())).toCompletableFuture().join();
        verify(ses).sendEmail(any(SendEmailRequest.class));verifyNoInteractions(sns,subscriptions,users);
    }
    @Test void reportUsesSesAndEachSnsEndpointWithIndependentLedger(){
        UUID userId=UUID.randomUUID();var user=io.github.KevinMitsi.animalesperdidos.domain.model.User.register(userId,
                "ana@example.com","hash","3000000000","123","Ana",Instant.parse("2026-08-13T10:00:00Z"));
        when(users.findById(userId)).thenReturn(done(Optional.of(user)));
        when(ledger.delivered(any(),eq("EMAIL"),eq("ana@example.com"))).thenReturn(done(false));
        when(ses.sendEmail(any(SendEmailRequest.class))).thenReturn(done(SendEmailResponse.builder().build()));
        when(ledger.mark(any(),eq("EMAIL"),eq("ana@example.com"))).thenReturn(done(null));
        when(subscriptions.endpointArns(userId)).thenReturn(done(List.of("arn:device:1","arn:device:2")));
        when(ledger.delivered(any(),eq("PUSH"),anyString())).thenReturn(done(false));
        when(sns.publish(any(PublishRequest.class))).thenReturn(done(PublishResponse.builder().build()));
        when(ledger.mark(any(),eq("PUSH"),anyString())).thenReturn(done(null));
        dispatcher.dispatch(new Event(UUID.randomUUID(),Type.LOST_PET_REPORT_CREATED,userId,null,
                Map.of("petName","Luna"),Instant.now())).toCompletableFuture().join();
        verify(sns,times(2)).publish(any(PublishRequest.class));verify(ledger).mark(any(),eq("PUSH"),eq("arn:device:1"));
        verify(ledger).mark(any(),eq("PUSH"),eq("arn:device:2"));
    }
    private static <T> CompletableFuture<T> done(T value){return CompletableFuture.completedFuture(value);}
}
