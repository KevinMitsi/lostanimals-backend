package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface CreateSightingUseCase {
    CompletionStage<Result> create(Command command);
    record Command(UUID reporterId, Species species, String description, Instant observedAt,
                   double latitude, double longitude, UUID neighborhoodId, List<String> imageKeys,
                   boolean confirmPossibleDuplicate) { public Command { imageKeys = List.copyOf(imageKeys); } }
    record Result(UUID sightingId, boolean created, DuplicateWarning warning) { }
    record DuplicateWarning(UUID existingSightingId, double distanceMeters, Instant observedAt) { }
}
