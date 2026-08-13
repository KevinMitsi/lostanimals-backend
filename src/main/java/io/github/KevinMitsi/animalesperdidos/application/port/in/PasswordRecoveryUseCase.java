package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.concurrent.CompletionStage;

public interface PasswordRecoveryUseCase {
    CompletionStage<Void> request(String email, String turnstileToken, String remoteIp);
    CompletionStage<Void> reset(String rawToken, String newPassword);
}
