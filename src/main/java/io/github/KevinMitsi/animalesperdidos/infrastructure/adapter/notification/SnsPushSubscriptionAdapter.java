package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PushSubscriptionPort;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.AwsNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="true")
public class SnsPushSubscriptionAdapter implements PushSubscriptionPort {
    private final SnsAsyncClient sns; private final DatabaseClient databaseClient;
    private final AwsNotificationProperties properties; private final Clock clock;
    @Override public CompletionStage<UUID> register(UUID userId,String deviceToken){
        if(deviceToken==null||deviceToken.isBlank()||deviceToken.length()>4096)
            throw new BusinessRuleViolation("Invalid push device token");
        CreatePlatformEndpointRequest request=CreatePlatformEndpointRequest.builder()
                .platformApplicationArn(requiredPlatformArn()).token(deviceToken.trim()).build();
        return sns.createPlatformEndpoint(request).thenCompose(response->{
            UUID id=UUID.randomUUID();
            return databaseClient.sql("""
                    INSERT INTO push_subscription(id,user_id,endpoint_arn,enabled,created_at,updated_at)
                    VALUES(:id,:user,:arn,true,:now,:now)
                    ON CONFLICT(endpoint_arn) DO UPDATE SET user_id=excluded.user_id,enabled=true,updated_at=excluded.updated_at
                    RETURNING id
                    """).bind("id",id).bind("user",userId).bind("arn",response.endpointArn()).bind("now",clock.instant())
                    .map((row,metadata)->row.get("id",UUID.class)).one().toFuture();
        });
    }
    @Override public CompletionStage<Void> remove(UUID userId,UUID subscriptionId){
        return databaseClient.sql("SELECT endpoint_arn FROM push_subscription WHERE id=:id AND user_id=:user")
                .bind("id",subscriptionId).bind("user",userId).map((row,metadata)->row.get("endpoint_arn",String.class)).one()
                .switchIfEmpty(reactor.core.publisher.Mono.error(new ResourceNotFound("Push subscription")))
                .flatMap(arn->reactor.core.publisher.Mono.fromCompletionStage(sns.deleteEndpoint(
                        DeleteEndpointRequest.builder().endpointArn(arn).build())).then(databaseClient.sql(
                                "DELETE FROM push_subscription WHERE id=:id AND user_id=:user")
                                .bind("id",subscriptionId).bind("user",userId).fetch().rowsUpdated()).then()).toFuture();
    }
    @Override public CompletionStage<List<String>> endpointArns(UUID userId){
        return databaseClient.sql("SELECT endpoint_arn FROM push_subscription WHERE user_id=:user AND enabled ORDER BY created_at")
                .bind("user",userId).map((row,metadata)->row.get("endpoint_arn",String.class)).all().collectList().toFuture();
    }
    @Override public CompletionStage<Void> disable(String endpointArn){
        return databaseClient.sql("UPDATE push_subscription SET enabled=false,updated_at=:now WHERE endpoint_arn=:arn")
                .bind("now",clock.instant()).bind("arn",endpointArn).fetch().rowsUpdated().then().toFuture();
    }
    private String requiredPlatformArn(){String value=properties.getPlatformApplicationArn();
        if(value==null||value.isBlank())throw new IllegalStateException("AWS_SNS_PLATFORM_APPLICATION_ARN is required");return value;}
}
