package io.github.kevinmitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.kevinmitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotificationAdapter implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public CompletionStage<Void> reportCreated(LostPetReport report) {
        log.info("Lost-pet report {} created for owner {}", report.id(), report.ownerId());
        return CompletableFuture.completedFuture(null);
    }
}
