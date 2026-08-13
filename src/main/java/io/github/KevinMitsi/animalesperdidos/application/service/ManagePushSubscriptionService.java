package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.ManagePushSubscriptionUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PushSubscriptionPort;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class ManagePushSubscriptionService implements ManagePushSubscriptionUseCase {
    private final PushSubscriptionPort subscriptions;
    public ManagePushSubscriptionService(PushSubscriptionPort subscriptions) { this.subscriptions=subscriptions; }
    @Override public CompletionStage<UUID> register(UUID userId,String deviceToken){return subscriptions.register(userId,deviceToken);}
    @Override public CompletionStage<Void> remove(UUID userId,UUID subscriptionId){return subscriptions.remove(userId,subscriptionId);}
}
