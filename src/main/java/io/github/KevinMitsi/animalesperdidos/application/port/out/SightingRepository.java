package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface SightingRepository {
    CompletionStage<Sighting> save(Sighting sighting);
    CompletionStage<Sighting> update(Sighting sighting);
    CompletionStage<Optional<Sighting>> findById(UUID id);
    CompletionStage<Optional<DuplicateCandidate>> findNearbyDuplicate(Species species, GeoPoint location,
                                                                       Instant from, Instant to, double meters);
    CompletionStage<List<Sighting>> search(SearchCriteria criteria);

    record DuplicateCandidate(UUID id, double distanceMeters, Instant observedAt) { }
    record SearchCriteria(UUID reporterId, Species species, String departmentCode, String municipalityCode, String neighborhood,
                          SightingStatus status, Instant from, Instant to, GeoSearchArea area,
                          boolean exactLocation, Instant cursorCreatedAt, UUID cursorId, int limit) { }
}
