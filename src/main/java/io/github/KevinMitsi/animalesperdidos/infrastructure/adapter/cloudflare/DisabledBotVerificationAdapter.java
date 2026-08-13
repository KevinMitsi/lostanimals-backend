package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.cloudflare;

import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@ConditionalOnProperty(name = "app.cloudflare.turnstile.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledBotVerificationAdapter implements BotVerificationPort {
    @Override
    public CompletionStage<Boolean> verify(String token, String remoteIp, String expectedAction) {
        return CompletableFuture.completedFuture(true);
    }
}
