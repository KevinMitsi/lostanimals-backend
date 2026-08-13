package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ManageSightingUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public final class ManageSightingService implements ManageSightingUseCase {
    private final SightingRepository repository; private final ImageStoragePort storage; private final Clock clock;
    private final ServiceAreaRepository serviceAreas;
    public ManageSightingService(SightingRepository repository, ImageStoragePort storage, Clock clock,
                                 ServiceAreaRepository serviceAreas) {
        this.repository = repository; this.storage = storage; this.clock = clock; this.serviceAreas = serviceAreas;
    }
    @Override public CompletionStage<Void> edit(UUID actorId, UUID id, Edit command) {
        if (command.observedAt().isAfter(clock.instant())) throw new BusinessRuleViolation("Observation cannot be in the future");
        return serviceAreas.isNeighborhoodEnabled(command.neighborhoodId()).thenCompose(enabled -> {
            if (!enabled) return SightingImagePolicy.failed(new BusinessRuleViolation("Publication area is not enabled"));
            return mutate(actorId, id, sighting -> sighting.edit(command.species(), command.description(), command.observedAt(),
                new GeoPoint(command.latitude(), command.longitude()), command.neighborhoodId(), clock.instant()))
                .thenApply(ignored -> null);
        });
    }
    @Override public CompletionStage<Void> close(UUID actorId, UUID id) {
        return mutate(actorId, id, sighting -> sighting.close(clock.instant())).thenApply(ignored -> null);
    }
    @Override public CompletionStage<UUID> addImage(UUID actorId, UUID id, String objectKey) {
        String prefix = "sightings/staging/users/" + actorId + "/";
        if (!objectKey.startsWith(prefix)) throw new BusinessRuleViolation("Image does not belong to the user");
        return storage.sanitize(actorId, ImageStoragePort.Category.SIGHTING, objectKey)
                .exceptionallyCompose(error -> SightingImagePolicy.failed(new BusinessRuleViolation("Image could not be validated")))
                .thenCompose(object -> {
                    if (!SightingImagePolicy.valid(object)) return SightingImagePolicy.failed(new BusinessRuleViolation("Invalid image"));
                    UUID imageId = UUID.randomUUID();
                    return mutate(actorId, id, sighting -> sighting.addImage(new SightingImage(imageId,
                            object.objectKey(), false, sighting.images().size()), clock.instant()))
                            .thenApply(ignored -> imageId).exceptionallyCompose(error -> storage.delete(object.objectKey())
                                    .thenCompose(ignored -> SightingImagePolicy.failed(error)));
                });
    }
    @Override public CompletionStage<Void> removeImage(UUID actorId, UUID id, UUID imageId) {
        return owned(actorId, id).thenCompose(sighting -> {
            String key = sighting.image(imageId).objectKey();
            return repository.update(sighting.removeImage(imageId, clock.instant()))
                    .thenCompose(updated -> storage.delete(key).exceptionally(error -> null)).thenApply(ignored -> null);
        });
    }
    @Override public CompletionStage<Void> setPrimary(UUID actorId, UUID id, UUID imageId) {
        return mutate(actorId, id, sighting -> sighting.setPrimary(imageId, clock.instant())).thenApply(ignored -> null);
    }
    private CompletionStage<Sighting> mutate(UUID actorId, UUID id, Function<Sighting,Sighting> fn) {
        return owned(actorId, id).thenCompose(value -> repository.update(fn.apply(value)));
    }
    private CompletionStage<Sighting> owned(UUID actorId, UUID id) {
        return repository.findById(id).thenCompose(optional -> {
            if (optional.isEmpty()) return SightingImagePolicy.failed(new ResourceNotFound("Sighting"));
            if (!optional.get().reporterId().equals(actorId)) return SightingImagePolicy.failed(new ForbiddenOperation());
            return CompletableFuture.completedFuture(optional.get());
        });
    }
}
