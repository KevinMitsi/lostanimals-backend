package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.model.*;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import java.util.*;
import java.util.concurrent.*;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="true")
public class AwsNotificationDispatcher {
    private final SesV2AsyncClient ses; private final SnsAsyncClient sns; private final UserRepository users;
    private final PushSubscriptionPort subscriptions; private final NotificationDeliveryLedger ledger;
    private final AwsNotificationProperties properties;

    public CompletionStage<Void> dispatch(Event event){
        return recipient(event).thenCompose(recipient->email(event,recipient))
                .thenCompose(ignored->event.type()==Type.LOST_PET_REPORT_CREATED&&event.recipientUserId()!=null
                        ? push(event):CompletableFuture.completedFuture(null));
    }
    private CompletionStage<Recipient> recipient(Event event){
        if(event.recipientEmail()!=null)return CompletableFuture.completedFuture(new Recipient(event.recipientEmail(),
                event.attributes().getOrDefault("displayName","")));
        return users.findById(event.recipientUserId()).thenCompose(user->user.<CompletionStage<Recipient>>map(value->
                CompletableFuture.completedFuture(new Recipient(value.email(),value.displayName())))
                .orElseGet(()->CompletableFuture.failedFuture(new IllegalStateException("Notification user not found"))));
    }
    private CompletionStage<Void> email(Event event,Recipient recipient){
        return ledger.delivered(event.id(),"EMAIL",recipient.email()).thenCompose(done->{
            if(done)return CompletableFuture.completedFuture(null);
            Email email=content(event,recipient);
            SendEmailRequest request=SendEmailRequest.builder().fromEmailAddress(required(properties.getSenderEmail(),"AWS_SES_SENDER_EMAIL"))
                    .destination(Destination.builder().toAddresses(recipient.email()).build())
                    .content(EmailContent.builder().simple(Message.builder()
                            .subject(Content.builder().data(email.subject()).charset("UTF-8").build())
                            .body(Body.builder().text(Content.builder().data(email.body()).charset("UTF-8").build()).build())
                            .build()).build()).build();
            return ses.sendEmail(request).thenCompose(ignored->ledger.mark(event.id(),"EMAIL",recipient.email()));
        });
    }
    private CompletionStage<Void> push(Event event){
        String message="Tu reporte de "+event.attributes().getOrDefault("petName","mascota")+" fue publicado.";
        return subscriptions.endpointArns(event.recipientUserId()).thenCompose(endpoints->{
            CompletableFuture<?>[] sends=endpoints.stream().map(endpoint->ledger.delivered(event.id(),"PUSH",endpoint)
                    .thenCompose(done->done?CompletableFuture.completedFuture(null):sns.publish(PublishRequest.builder()
                            .targetArn(endpoint).subject("Animales Perdidos Colombia").message(message).build())
                            .thenCompose(ignored->ledger.mark(event.id(),"PUSH",endpoint))
                            .exceptionallyCompose(error->endpointDisabled(error)
                                    ? subscriptions.disable(endpoint) : CompletableFuture.failedFuture(unwrap(error))))
                    .toCompletableFuture()).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(sends);
        });
    }
    private Email content(Event event,Recipient recipient){
        String name=event.attributes().getOrDefault("displayName",recipient.name());
        return switch(event.type()){
            case EMAIL_VERIFICATION->new Email("Verifica tu correo","Hola "+name+", verifica tu correo en: "+
                    properties.getFrontendBaseUrl()+"/verify-email?token="+required(event.attributes().get("token"),"event token"));
            case PASSWORD_RESET->new Email("Recupera tu contraseña","Hola "+name+", cambia tu contraseña en: "+
                    properties.getFrontendBaseUrl()+"/reset-password?token="+required(event.attributes().get("token"),"event token"));
            case LOST_PET_REPORT_CREATED->new Email("Reporte de mascota publicado","El reporte de "+
                    event.attributes().getOrDefault("petName","tu mascota")+" fue publicado correctamente.");
        };
    }
    private static String required(String value,String name){if(value==null||value.isBlank())throw new IllegalStateException(name+" is required");return value;}
    private static boolean endpointDisabled(Throwable error){return unwrap(error) instanceof software.amazon.awssdk.services.sns.model.EndpointDisabledException;}
    private static Throwable unwrap(Throwable error){return error instanceof CompletionException&&error.getCause()!=null?error.getCause():error;}
    private record Recipient(String email,String name){} private record Email(String subject,String body){}
}
