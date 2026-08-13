package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ManagePushSubscriptionUseCase {
    CompletionStage<UUID> register(UUID userId, String deviceToken);
    CompletionStage<Void> remove(UUID userId, UUID subscriptionId);
}
