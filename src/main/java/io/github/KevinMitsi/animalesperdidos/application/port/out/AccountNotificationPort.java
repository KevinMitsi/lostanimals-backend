package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.concurrent.CompletionStage;

public interface AccountNotificationPort {
    CompletionStage<Void> sendEmailVerification(String email, String displayName, String rawToken);
    CompletionStage<Void> sendPasswordReset(String email, String displayName, String rawToken);
}
