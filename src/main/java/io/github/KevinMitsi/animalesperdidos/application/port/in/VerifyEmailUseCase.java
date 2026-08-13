package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.concurrent.CompletionStage;

public interface VerifyEmailUseCase {
    CompletionStage<Void> verify(String rawToken);
    CompletionStage<Void> resend(String email, String turnstileToken, String remoteIp);
}
