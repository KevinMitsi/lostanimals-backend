package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import io.github.KevinMitsi.animalesperdidos.domain.model.AdministrativeLocation;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ManageSightingUseCase {
    CompletionStage<Void> edit(UUID actorId, UUID id, Edit command);
    CompletionStage<Void> close(UUID actorId, UUID id);
    CompletionStage<UUID> addImage(UUID actorId, UUID id, String objectKey);
    CompletionStage<Void> removeImage(UUID actorId, UUID id, UUID imageId);
    CompletionStage<Void> setPrimary(UUID actorId, UUID id, UUID imageId);
    record Edit(Species species, String description, Instant observedAt, double latitude,
                double longitude, AdministrativeLocation administrativeLocation) { }
}
