package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CompleteGoogleProfileUseCase {
    CompletionStage<Result> complete(Command command);

    record Command(UUID userId, String phone, String documentNumber) {
    }

    record Result(UUID userId, String email, String displayName, String phone, String documentNumber,
                  String pictureUrl, boolean profileComplete) {
    }
}
