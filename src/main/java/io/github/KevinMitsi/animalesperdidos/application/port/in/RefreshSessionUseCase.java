package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.concurrent.CompletionStage;

public interface RefreshSessionUseCase {
    CompletionStage<Result> refresh(String refreshToken);
    CompletionStage<Void> logout(String refreshToken);

    record Result(String accessToken, String refreshToken, String tokenType,
                  long expiresInSeconds, long refreshExpiresInSeconds) { }
}
