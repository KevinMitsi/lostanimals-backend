package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;
import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PushSubscriptionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="false",matchIfMissing=true)
public class UnavailablePushSubscriptionAdapter implements PushSubscriptionPort {
    @Override public CompletionStage<UUID> register(UUID userId,String token){return failed();}
    @Override public CompletionStage<Void> remove(UUID userId,UUID id){return failed();}
    @Override public CompletionStage<List<String>> endpointArns(UUID userId){return CompletableFuture.completedFuture(List.of());}
    @Override public CompletionStage<Void> disable(String arn){return CompletableFuture.completedFuture(null);}
    private static <T> CompletionStage<T> failed(){return CompletableFuture.failedFuture(
            new BusinessRuleViolation("Push notifications are not enabled"));}
}
