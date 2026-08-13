package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface RegisterUserUseCase {
    CompletionStage<Result> register(Command command);

    record Command(String email, String password, String phone, String documentNumber,
                   String displayName, boolean acceptsDataProcessing, String turnstileToken, String remoteIp) {
    }

    record Result(UUID userId, String email) {
    }
}
