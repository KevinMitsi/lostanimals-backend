package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;

import java.util.concurrent.CompletionStage;

public interface NotificationPort {

    CompletionStage<Void> reportCreated(LostPetReport report);
}
