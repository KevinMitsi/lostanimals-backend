package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface LostPetReportRepository {

    CompletionStage<Boolean> existsActiveDuplicate(UUID ownerId, Species species, String petName, Instant since);

    CompletionStage<Long> countCreatedByOwnerSince(UUID ownerId, Instant since);

    CompletionStage<LostPetReport> save(LostPetReport report);

    CompletionStage<Optional<LostPetReport>> findById(UUID reportId);

    CompletionStage<LostPetReport> update(LostPetReport report);

    CompletionStage<List<LostPetReport>> search(SearchCriteria criteria);

    record SearchCriteria(UUID ownerId, Species species, UUID departmentId, UUID cityId, UUID neighborhoodId,
                          io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus status,
                          Instant from, Instant to, io.github.KevinMitsi.animalesperdidos.domain.model.GeoSearchArea area,
                          boolean exactLocation, Instant cursorCreatedAt, UUID cursorId, int limit) { }
}
