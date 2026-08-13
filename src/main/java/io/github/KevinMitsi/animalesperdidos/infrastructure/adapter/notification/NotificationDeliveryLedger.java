package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Component @RequiredArgsConstructor
public class NotificationDeliveryLedger {
    private final DatabaseClient databaseClient; private final Clock clock;
    public CompletionStage<Boolean> delivered(UUID eventId,String channel,String target){
        return databaseClient.sql("SELECT EXISTS(SELECT 1 FROM notification_delivery WHERE event_id=:id AND channel=:channel AND target=:target) present")
                .bind("id",eventId).bind("channel",channel).bind("target",target)
                .map((row,metadata)->Boolean.TRUE.equals(row.get("present",Boolean.class)))
                .one().defaultIfEmpty(false).toFuture();
    }
    public CompletionStage<Void> mark(UUID eventId,String channel,String target){
        return databaseClient.sql("""
                INSERT INTO notification_delivery(event_id,channel,target,delivered_at) VALUES(:id,:channel,:target,:at)
                ON CONFLICT(event_id,channel,target) DO NOTHING
                """).bind("id",eventId).bind("channel",channel).bind("target",target).bind("at",clock.instant())
                .fetch().rowsUpdated().then().toFuture();
    }
}
