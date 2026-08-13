package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.*;
import java.util.concurrent.CompletionStage;

public interface PushSubscriptionPort {
    CompletionStage<UUID> register(UUID userId, String deviceToken);
    CompletionStage<Void> remove(UUID userId, UUID subscriptionId);
    CompletionStage<List<String>> endpointArns(UUID userId);
    CompletionStage<Void> disable(String endpointArn);
}
