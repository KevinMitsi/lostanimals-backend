package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.concurrent.CompletionStage;

public interface AuthenticateUserUseCase {
    CompletionStage<Result> authenticate(Command command);

    record Command(String email, String password, String turnstileToken, String remoteIp) {
    }

    record Result(String accessToken, String refreshToken, String tokenType,
                  long expiresInSeconds, long refreshExpiresInSeconds) {
    }
}
