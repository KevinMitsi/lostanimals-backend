package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.concurrent.CompletionStage;

public interface GoogleIdentityPort {
    CompletionStage<Identity> verify(String credential);

    record Identity(String subject, String email, boolean emailVerified, String displayName, String pictureUrl) {
    }
}
