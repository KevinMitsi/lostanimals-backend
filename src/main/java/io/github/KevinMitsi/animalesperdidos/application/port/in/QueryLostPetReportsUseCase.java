package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface QueryLostPetReportsUseCase {
    CompletionStage<ReportView> getPublic(UUID reportId);
    CompletionStage<Page> searchPublic(Search command);
    CompletionStage<Page> mine(UUID ownerId, Search command);

    record Search(Species species, UUID neighborhoodId, ReportStatus status,
                  Instant cursorCreatedAt, UUID cursorId, int limit) { }

    record Page(List<ReportView> items, String nextCursor) { }

    record ReportView(UUID id, String petName, Species species, String description,
                      Instant disappearedAt, double latitude, double longitude, UUID neighborhoodId,
                      ReportStatus status, List<ImageView> images, Instant createdAt,
                      Instant updatedAt, long version) { }

    record ImageView(UUID id, String url, boolean primary, int sortOrder) { }
}
