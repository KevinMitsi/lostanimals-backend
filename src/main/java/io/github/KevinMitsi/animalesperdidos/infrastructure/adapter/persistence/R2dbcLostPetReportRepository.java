package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.ConcurrentUpdate;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcLostPetReportRepository implements LostPetReportRepository {
    private static final String SELECT_REPORT = """
            SELECT r.id, r.owner_id, r.pet_name, r.species, r.description, r.disappeared_at,
                   ST_Y(r.last_seen::geometry) AS latitude, ST_X(r.last_seen::geometry) AS longitude,
                   r.department_code, r.municipality_code, r.neighborhood,
                   r.status, r.created_at, r.updated_at, r.version,
                   i.id AS image_id, i.object_key, i.is_primary, i.sort_order
            FROM lost_pet_report r
            LEFT JOIN lost_pet_image i ON i.report_id = r.id
            """;

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transaction;

    @Override
    public CompletionStage<Boolean> existsActiveDuplicate(UUID ownerId, Species species, String petName, Instant since) {
        return databaseClient.sql("""
                SELECT EXISTS(SELECT 1 FROM lost_pet_report
                WHERE owner_id=:ownerId AND species=:species AND lower(pet_name)=lower(:petName)
                  AND status='LOST' AND created_at>=:since) AS present
                """).bind("ownerId", ownerId).bind("species", species.name()).bind("petName", petName)
                .bind("since", since).map((row, metadata) -> Boolean.TRUE.equals(row.get("present", Boolean.class)))
                .one().defaultIfEmpty(false).toFuture();
    }

    @Override
    public CompletionStage<Long> countCreatedByOwnerSince(UUID ownerId, Instant since) {
        return databaseClient.sql("SELECT count(*) AS report_count FROM lost_pet_report WHERE owner_id=:ownerId AND created_at>=:since")
                .bind("ownerId", ownerId).bind("since", since)
                .map((row, metadata) -> row.get("report_count", Long.class)).one().defaultIfEmpty(0L).toFuture();
    }

    @Override
    public CompletionStage<LostPetReport> save(LostPetReport report) {
        Mono<Long> reportInsert = databaseClient.sql("""
                INSERT INTO lost_pet_report(id, owner_id, pet_name, species, description, disappeared_at,
                    last_seen, department_code, municipality_code, neighborhood,
                    status, created_at, updated_at, version)
                VALUES (:id,:ownerId,:petName,:species,:description,:disappearedAt,
                    ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,
                    :departmentCode,:municipalityCode,:neighborhood,:status,:createdAt,:updatedAt,:version)
                """).bind("id", report.id()).bind("ownerId", report.ownerId()).bind("petName", report.petName())
                .bind("species", report.species().name()).bind("description", report.description())
                .bind("disappearedAt", report.disappearedAt()).bind("longitude", report.lastSeenAt().longitude())
                .bind("latitude", report.lastSeenAt().latitude())
                .bind("departmentCode", report.administrativeLocation().departmentCode())
                .bind("municipalityCode", report.administrativeLocation().municipalityCode())
                .bind("neighborhood", report.administrativeLocation().neighborhood())
                .bind("status", report.status().name()).bind("createdAt", report.createdAt())
                .bind("updatedAt", report.updatedAt()).bind("version", report.version()).fetch().rowsUpdated();
        return transaction.transactional(reportInsert.thenMany(insertImages(report)).then(Mono.just(report))).toFuture();
    }

    @Override
    public CompletionStage<Optional<LostPetReport>> findById(UUID reportId) {
        return aggregate(databaseClient.sql(SELECT_REPORT + " WHERE r.id=:id ORDER BY i.sort_order")
                .bind("id", reportId).map(this::row).all()).map(list -> list.stream().findFirst()).toFuture();
    }

    @Override
    public CompletionStage<LostPetReport> update(LostPetReport report) {
        Mono<Long> update = databaseClient.sql("""
                UPDATE lost_pet_report SET pet_name=:petName, species=:species, description=:description,
                    disappeared_at=:disappearedAt,
                    last_seen=ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,
                    department_code=:departmentCode, municipality_code=:municipalityCode,
                    neighborhood=:neighborhood, status=:status, updated_at=:updatedAt, version=version+1
                WHERE id=:id AND version=:version
                """).bind("petName", report.petName()).bind("species", report.species().name())
                .bind("description", report.description()).bind("disappearedAt", report.disappearedAt())
                .bind("longitude", report.lastSeenAt().longitude()).bind("latitude", report.lastSeenAt().latitude())
                .bind("departmentCode", report.administrativeLocation().departmentCode())
                .bind("municipalityCode", report.administrativeLocation().municipalityCode())
                .bind("neighborhood", report.administrativeLocation().neighborhood()).bind("status", report.status().name())
                .bind("updatedAt", report.updatedAt()).bind("id", report.id()).bind("version", report.version())
                .fetch().rowsUpdated().flatMap(rows -> rows == 1 ? Mono.just(rows) : Mono.error(new ConcurrentUpdate()));
        Mono<Long> deleteImages = databaseClient.sql("DELETE FROM lost_pet_image WHERE report_id=:reportId")
                .bind("reportId", report.id()).fetch().rowsUpdated();
        LostPetReport updated = new LostPetReport(report.id(), report.ownerId(), report.petName(), report.species(),
                report.description(), report.disappearedAt(), report.lastSeenAt(), report.administrativeLocation(),
                report.status(), report.images(), report.createdAt(), report.updatedAt(), report.version() + 1);
        return transaction.transactional(update.then(deleteImages).thenMany(insertImages(updated)).then(Mono.just(updated))).toFuture();
    }

    @Override
    public CompletionStage<List<LostPetReport>> search(SearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (criteria.ownerId() != null) where.append(" AND r.owner_id=:ownerId");
        if (criteria.species() != null) where.append(" AND r.species=:species");
        if (criteria.departmentCode() != null) where.append(" AND r.department_code=:departmentCode");
        if (criteria.municipalityCode() != null) where.append(" AND r.municipality_code=:municipalityCode");
        if (criteria.neighborhood() != null) where.append(" AND lower(r.neighborhood)=lower(:neighborhood)");
        if (criteria.status() != null) where.append(" AND r.status=:status");
        if (!criteria.exactLocation() && criteria.status() == null) where.append(" AND r.status='LOST'");
        if (criteria.from() != null) where.append(" AND r.disappeared_at>=:from");
        if (criteria.to() != null) where.append(" AND r.disappeared_at<=:to");
        if (criteria.area() != null) where.append(criteria.exactLocation() ? """
                 AND ST_DWithin(r.last_seen,
                   ST_SetSRID(ST_MakePoint(:centerLongitude,:centerLatitude),4326)::geography,:radiusMeters)
                """ : """
                 AND ST_DWithin(r.last_seen,
                   ST_SetSRID(ST_MakePoint(:centerLongitude,:centerLatitude),4326)::geography,:prefilterRadius)
                 AND ST_DWithin(ST_SetSRID(ST_MakePoint(
                   round(ST_X(r.last_seen::geometry)::numeric,3)::double precision,
                   round(ST_Y(r.last_seen::geometry)::numeric,3)::double precision),4326)::geography,
                   ST_SetSRID(ST_MakePoint(:centerLongitude,:centerLatitude),4326)::geography,:radiusMeters)
                """);
        if (criteria.cursorCreatedAt() != null && criteria.cursorId() != null) {
            where.append(" AND (r.created_at,r.id)<(:cursorCreatedAt,:cursorId)");
        }
        String idsSql = "SELECT r.id FROM lost_pet_report r" + where
                + " ORDER BY r.created_at DESC,r.id DESC LIMIT :limit";
        DatabaseClient.GenericExecuteSpec ids = databaseClient.sql(idsSql);
        ids = bindCriteria(ids, criteria).bind("limit", criteria.limit());
        Flux<UUID> selectedIds = ids.map((row, metadata) -> row.get("id", UUID.class)).all();
        return selectedIds.collectList().flatMap(idsList -> {
            if (idsList.isEmpty()) return Mono.<List<LostPetReport>>just(List.of());
            return aggregate(databaseClient.sql(SELECT_REPORT + " WHERE r.id IN (:ids) ORDER BY r.created_at DESC,r.id DESC,i.sort_order")
                    .bind("ids", idsList).map(this::row).all());
        }).toFuture();
    }

    private DatabaseClient.GenericExecuteSpec bindCriteria(DatabaseClient.GenericExecuteSpec spec, SearchCriteria criteria) {
        if (criteria.ownerId() != null) spec = spec.bind("ownerId", criteria.ownerId());
        if (criteria.species() != null) spec = spec.bind("species", criteria.species().name());
        if (criteria.departmentCode() != null) spec = spec.bind("departmentCode", criteria.departmentCode());
        if (criteria.municipalityCode() != null) spec = spec.bind("municipalityCode", criteria.municipalityCode());
        if (criteria.neighborhood() != null) spec = spec.bind("neighborhood", criteria.neighborhood());
        if (criteria.status() != null) spec = spec.bind("status", criteria.status().name());
        if (criteria.from() != null) spec = spec.bind("from", criteria.from());
        if (criteria.to() != null) spec = spec.bind("to", criteria.to());
        if (criteria.area() != null) spec = spec.bind("centerLongitude", criteria.area().center().longitude())
                .bind("centerLatitude", criteria.area().center().latitude())
                .bind("radiusMeters", criteria.area().radiusMeters());
        if (criteria.area() != null && !criteria.exactLocation()) {
            spec = spec.bind("prefilterRadius", criteria.area().radiusMeters() + 100d);
        }
        if (criteria.cursorCreatedAt() != null && criteria.cursorId() != null) {
            spec = spec.bind("cursorCreatedAt", criteria.cursorCreatedAt()).bind("cursorId", criteria.cursorId());
        }
        return spec;
    }

    private Flux<Long> insertImages(LostPetReport report) {
        return Flux.fromIterable(report.images()).concatMap(image -> databaseClient.sql("""
                INSERT INTO lost_pet_image(id,report_id,object_key,is_primary,sort_order)
                VALUES (:id,:reportId,:key,:primary,:sortOrder)
                """).bind("id", image.id()).bind("reportId", report.id()).bind("key", image.objectKey())
                .bind("primary", image.primary()).bind("sortOrder", image.sortOrder()).fetch().rowsUpdated());
    }

    private ReportRow row(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        UUID imageId = row.get("image_id", UUID.class);
        LostPetImage image = imageId == null ? null : new LostPetImage(imageId, row.get("object_key", String.class),
                Boolean.TRUE.equals(row.get("is_primary", Boolean.class)), row.get("sort_order", Integer.class));
        return new ReportRow(row.get("id", UUID.class), row.get("owner_id", UUID.class), row.get("pet_name", String.class),
                Species.valueOf(row.get("species", String.class)), row.get("description", String.class),
                row.get("disappeared_at", Instant.class), row.get("latitude", Double.class), row.get("longitude", Double.class),
                row.get("department_code", String.class), row.get("municipality_code", String.class),
                row.get("neighborhood", String.class), ReportStatus.valueOf(row.get("status", String.class)),
                row.get("created_at", Instant.class), row.get("updated_at", Instant.class),
                row.get("version", Long.class), image);
    }

    private Mono<List<LostPetReport>> aggregate(Flux<ReportRow> rows) {
        return rows.collectList().map(all -> {
            Map<UUID, List<ReportRow>> grouped = new LinkedHashMap<>();
            all.forEach(row -> grouped.computeIfAbsent(row.id(), ignored -> new ArrayList<>()).add(row));
            return grouped.values().stream().map(group -> {
                ReportRow first = group.getFirst();
                List<LostPetImage> images = group.stream().map(ReportRow::image).filter(Objects::nonNull).toList();
                return new LostPetReport(first.id(), first.ownerId(), first.petName(), first.species(), first.description(),
                        first.disappearedAt(), new GeoPoint(first.latitude(), first.longitude()),
                        new AdministrativeLocation(first.departmentCode(), first.municipalityCode(), first.neighborhood()),
                        first.status(), images, first.createdAt(), first.updatedAt(), first.version());
            }).toList();
        });
    }

    private record ReportRow(UUID id, UUID ownerId, String petName, Species species, String description,
                             Instant disappearedAt, double latitude, double longitude, String departmentCode,
                             String municipalityCode, String neighborhood,
                             ReportStatus status, Instant createdAt, Instant updatedAt, long version,
                             LostPetImage image) { }
}
