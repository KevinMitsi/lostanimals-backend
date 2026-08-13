package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.concurrent.CompletionStage;

public interface BotVerificationPort {
    CompletionStage<Boolean> verify(String token, String remoteIp, String expectedAction);
}
