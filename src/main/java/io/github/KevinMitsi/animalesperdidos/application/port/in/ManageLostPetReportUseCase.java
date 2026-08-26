package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import io.github.KevinMitsi.animalesperdidos.domain.model.AdministrativeLocation;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ManageLostPetReportUseCase {
    CompletionStage<Void> edit(UUID actorId, UUID reportId, Edit command);
    CompletionStage<Void> close(UUID actorId, UUID reportId, ReportStatus status);
    CompletionStage<UUID> addImage(UUID actorId, UUID reportId, String objectKey);
    CompletionStage<Void> removeImage(UUID actorId, UUID reportId, UUID imageId);
    CompletionStage<Void> setPrimaryImage(UUID actorId, UUID reportId, UUID imageId);

    record Edit(String petName, Species species, String description, Instant disappearedAt,
                double latitude, double longitude, AdministrativeLocation administrativeLocation) { }
}
