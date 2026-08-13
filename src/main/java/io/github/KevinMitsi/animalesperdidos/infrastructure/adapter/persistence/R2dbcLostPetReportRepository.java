package io.github.kevinmitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.kevinmitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.kevinmitsi.animalesperdidos.domain.model.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcLostPetReportRepository implements LostPetReportRepository {
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transaction;

    @Override
    public CompletionStage<Boolean> existsActiveDuplicate(UUID ownerId, Species species, String petName, Instant since) {
        return databaseClient.sql("""
                        SELECT EXISTS(
                            SELECT 1 FROM lost_pet_report
                            WHERE owner_id = :ownerId AND species = :species AND lower(pet_name) = lower(:petName)
                              AND status = 'LOST' AND created_at >= :since
                        ) AS present
                        """)
                .bind("ownerId", ownerId)
                .bind("species", species.name())
                .bind("petName", petName)
                .bind("since", since)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("present", Boolean.class)))
                .one()
                .defaultIfEmpty(false)
                .toFuture();
    }

    @Override
    public CompletionStage<Long> countCreatedByOwnerSince(UUID ownerId, Instant since) {
        return databaseClient.sql("""
                        SELECT count(*) AS report_count FROM lost_pet_report
                        WHERE owner_id = :ownerId AND created_at >= :since
                        """)
                .bind("ownerId", ownerId)
                .bind("since", since)
                .map((row, metadata) -> row.get("report_count", Long.class))
                .one()
                .defaultIfEmpty(0L)
                .toFuture();
    }

    @Override
    public CompletionStage<LostPetReport> save(LostPetReport report) {
        Mono<Long> insertReport = databaseClient.sql("""
                        INSERT INTO lost_pet_report
                            (id, owner_id, pet_name, species, description, disappeared_at, last_seen,
                             neighborhood_id, status, created_at)
                        VALUES (:id, :ownerId, :petName, :species, :description, :disappearedAt,
                            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                            :neighborhoodId, :status, :createdAt)
                        """)
                .bind("id", report.id())
                .bind("ownerId", report.ownerId())
                .bind("petName", report.petName())
                .bind("species", report.species().name())
                .bind("description", report.description())
                .bind("disappearedAt", report.disappearedAt())
                .bind("longitude", report.lastSeenAt().longitude())
                .bind("latitude", report.lastSeenAt().latitude())
                .bind("neighborhoodId", report.neighborhoodId())
                .bind("status", report.status().name())
                .bind("createdAt", report.createdAt())
                .fetch().rowsUpdated();

        Flux<Long> insertImages = Flux.fromIterable(report.imageKeys())
                .index()
                .concatMap(indexed -> databaseClient.sql("""
                                INSERT INTO lost_pet_image (id, report_id, object_key, is_primary, sort_order)
                                VALUES (:id, :reportId, :objectKey, :isPrimary, :sortOrder)
                                """)
                        .bind("id", UUID.randomUUID())
                        .bind("reportId", report.id())
                        .bind("objectKey", indexed.getT2())
                        .bind("isPrimary", indexed.getT1() == 0)
                        .bind("sortOrder", indexed.getT1().intValue())
                        .fetch().rowsUpdated());

        return transaction.transactional(insertReport.thenMany(insertImages).then(Mono.just(report))).toFuture();
    }
}
