package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.concurrent.CompletionStage;

public interface GoogleAuthenticationUseCase {
    CompletionStage<Result> authenticate(Command command);

    record Command(String credential, boolean acceptsDataProcessing) {
    }

    record Result(String accessToken, String refreshToken, String tokenType, long expiresInSeconds,
                  long refreshExpiresInSeconds, boolean profileComplete, boolean newUser) {
    }
}
