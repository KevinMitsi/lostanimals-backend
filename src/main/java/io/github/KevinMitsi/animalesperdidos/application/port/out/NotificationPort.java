package io.github.kevinmitsi.animalesperdidos.application.port.out;

import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;

import java.util.concurrent.CompletionStage;

public interface NotificationPort {

    CompletionStage<Void> reportCreated(LostPetReport report);
}
