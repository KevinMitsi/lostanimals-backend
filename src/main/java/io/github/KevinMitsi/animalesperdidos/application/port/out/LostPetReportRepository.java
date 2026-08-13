package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface LostPetReportRepository {

    CompletionStage<Boolean> existsActiveDuplicate(UUID ownerId, Species species, String petName, Instant since);

    CompletionStage<Long> countCreatedByOwnerSince(UUID ownerId, Instant since);

    CompletionStage<LostPetReport> save(LostPetReport report);
}
