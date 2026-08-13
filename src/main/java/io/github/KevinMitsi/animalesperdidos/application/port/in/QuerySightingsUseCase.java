package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface QuerySightingsUseCase {
    CompletionStage<View> getPublic(UUID id);
    CompletionStage<Page> searchPublic(Search search);
    CompletionStage<Page> mine(UUID reporterId, Search search);
    record Search(Species species, UUID neighborhoodId, SightingStatus status,
                  Instant cursorCreatedAt, UUID cursorId, int limit) { }
    record Page(List<View> items, String nextCursor) { }
    record View(UUID id, Species species, String description, Instant observedAt, double latitude,
                double longitude, UUID neighborhoodId, SightingStatus status, List<ImageView> images,
                Instant createdAt, Instant updatedAt, long version) { }
    record ImageView(UUID id, String url, boolean primary, int sortOrder) { }
}
