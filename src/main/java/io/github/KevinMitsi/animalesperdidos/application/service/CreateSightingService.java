package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.CreateSightingUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class CreateSightingService implements CreateSightingUseCase {
    private final SightingRepository repository; private final ImageStoragePort storage; private final Clock clock;
    private final ServiceAreaRepository serviceAreas;
    public CreateSightingService(SightingRepository repository, ImageStoragePort storage, Clock clock,
                                 ServiceAreaRepository serviceAreas) {
        this.repository = repository; this.storage = storage; this.clock = clock; this.serviceAreas = serviceAreas;
    }
    @Override public CompletionStage<Result> create(Command command) {
        Instant now = clock.instant();
        if (command.observedAt().isAfter(now)) throw new BusinessRuleViolation("Observation cannot be in the future");
        GeoPoint point = new GeoPoint(command.latitude(), command.longitude());
        return serviceAreas.isNeighborhoodEnabled(command.neighborhoodId()).thenCompose(enabled -> {
            if (!enabled) return SightingImagePolicy.failed(new BusinessRuleViolation("Publication area is not enabled"));
            return repository.findNearbyDuplicate(command.species(), point, command.observedAt().minus(Duration.ofHours(2)),
                        command.observedAt().plus(Duration.ofHours(2)), 50)
                .thenCompose(candidate -> {
                    if (candidate.isPresent() && !command.confirmPossibleDuplicate()) {
                        var value = candidate.get();
                        return CompletableFuture.completedFuture(new Result(null, false,
                                new DuplicateWarning(value.id(), value.distanceMeters(), value.observedAt())));
                    }
                    return SightingImagePolicy.sanitize(storage, command.reporterId(), command.imageKeys())
                            .thenCompose(keys -> persist(command, point, keys, now));
                });
        });
    }
    private CompletionStage<Result> persist(Command command, GeoPoint point, List<String> keys, Instant now) {
        Sighting sighting = Sighting.create(UUID.randomUUID(), command.reporterId(), command.species(),
                command.description(), command.observedAt(), point, command.neighborhoodId(), keys, now);
        return repository.save(sighting)
                .exceptionallyCompose(error -> SightingImagePolicy.delete(storage, keys)
                        .thenCompose(ignored -> SightingImagePolicy.failed(error)))
                .thenApply(saved -> new Result(saved.id(), true, null));
    }
}
