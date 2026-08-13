package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.ConcurrentUpdate;
import io.github.KevinMitsi.animalesperdidos.application.port.out.SightingRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository @RequiredArgsConstructor
public class R2dbcSightingRepository implements SightingRepository {
    private static final String SELECT = """
            SELECT s.id,s.reporter_id,s.species,s.description,s.observed_at,
              ST_Y(s.location::geometry) latitude,ST_X(s.location::geometry) longitude,
              s.neighborhood_id,s.status,s.created_at,s.updated_at,s.version,
              i.id image_id,i.object_key,i.is_primary,i.sort_order
            FROM sighting s LEFT JOIN sighting_image i ON i.sighting_id=s.id
            """;
    private final DatabaseClient databaseClient; private final TransactionalOperator transaction;

    @Override public CompletionStage<Sighting> save(Sighting s) {
        Mono<Long> insert = databaseClient.sql("""
                INSERT INTO sighting(id,reporter_id,species,description,observed_at,location,neighborhood_id,
                  status,created_at,updated_at,version)
                VALUES(:id,:reporter,:species,:description,:observed,
                  ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,
                  :neighborhood,:status,:created,:updated,:version)
                """).bind("id",s.id()).bind("reporter",s.reporterId()).bind("species",s.species().name())
                .bind("description",s.description()).bind("observed",s.observedAt())
                .bind("longitude",s.location().longitude()).bind("latitude",s.location().latitude())
                .bind("neighborhood",s.neighborhoodId()).bind("status",s.status().name())
                .bind("created",s.createdAt()).bind("updated",s.updatedAt()).bind("version",s.version())
                .fetch().rowsUpdated();
        return transaction.transactional(insert.thenMany(insertImages(s)).then(Mono.just(s))).toFuture();
    }
    @Override public CompletionStage<Sighting> update(Sighting s) {
        Mono<Long> update = databaseClient.sql("""
                UPDATE sighting SET species=:species,description=:description,observed_at=:observed,
                  location=ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,
                  neighborhood_id=:neighborhood,status=:status,updated_at=:updated,version=version+1
                WHERE id=:id AND version=:version
                """).bind("species",s.species().name()).bind("description",s.description())
                .bind("observed",s.observedAt()).bind("longitude",s.location().longitude())
                .bind("latitude",s.location().latitude()).bind("neighborhood",s.neighborhoodId())
                .bind("status",s.status().name()).bind("updated",s.updatedAt()).bind("id",s.id())
                .bind("version",s.version()).fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? Mono.just(rows) : Mono.error(new ConcurrentUpdate()));
        Mono<Long> delete = databaseClient.sql("DELETE FROM sighting_image WHERE sighting_id=:id")
                .bind("id",s.id()).fetch().rowsUpdated();
        Sighting result = new Sighting(s.id(),s.reporterId(),s.species(),s.description(),s.observedAt(),s.location(),
                s.neighborhoodId(),s.status(),s.images(),s.createdAt(),s.updatedAt(),s.version()+1);
        return transaction.transactional(update.then(delete).thenMany(insertImages(result)).then(Mono.just(result))).toFuture();
    }
    @Override public CompletionStage<Optional<Sighting>> findById(UUID id) {
        return aggregate(databaseClient.sql(SELECT+" WHERE s.id=:id ORDER BY i.sort_order").bind("id",id)
                .map(this::row).all()).map(list -> list.stream().findFirst()).toFuture();
    }
    @Override public CompletionStage<Optional<DuplicateCandidate>> findNearbyDuplicate(Species species, GeoPoint point,
                                                                                       Instant from, Instant to, double meters) {
        return databaseClient.sql("""
                SELECT id,observed_at,ST_Distance(location,
                  ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography) distance
                FROM sighting WHERE species=:species AND status='ACTIVE' AND observed_at BETWEEN :from AND :to
                  AND ST_DWithin(location,ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,:meters)
                ORDER BY distance,observed_at DESC LIMIT 1
                """).bind("longitude",point.longitude()).bind("latitude",point.latitude())
                .bind("species",species.name()).bind("from",from).bind("to",to).bind("meters",meters)
                .map((row,meta) -> new DuplicateCandidate(row.get("id",UUID.class),row.get("distance",Double.class),
                        row.get("observed_at",Instant.class))).one().map(Optional::of)
                .defaultIfEmpty(Optional.empty()).toFuture();
    }
    @Override public CompletionStage<List<Sighting>> search(SearchCriteria c) {
        StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(c.reporterId()!=null)where.append(" AND s.reporter_id=:reporter");
        if(c.species()!=null)where.append(" AND s.species=:species");
        if(c.neighborhoodId()!=null)where.append(" AND s.neighborhood_id=:neighborhood");
        if(c.status()!=null)where.append(" AND s.status=:status");
        if(c.cursorCreatedAt()!=null&&c.cursorId()!=null)where.append(" AND (s.created_at,s.id)<(:cursorAt,:cursorId)");
        var spec=databaseClient.sql("SELECT s.id FROM sighting s"+where+" ORDER BY s.created_at DESC,s.id DESC LIMIT :limit");
        if(c.reporterId()!=null)spec=spec.bind("reporter",c.reporterId());
        if(c.species()!=null)spec=spec.bind("species",c.species().name());
        if(c.neighborhoodId()!=null)spec=spec.bind("neighborhood",c.neighborhoodId());
        if(c.status()!=null)spec=spec.bind("status",c.status().name());
        if(c.cursorCreatedAt()!=null&&c.cursorId()!=null)spec=spec.bind("cursorAt",c.cursorCreatedAt()).bind("cursorId",c.cursorId());
        return spec.bind("limit",c.limit()).map((row,meta)->row.get("id",UUID.class)).all().collectList()
                .flatMap(ids -> ids.isEmpty()?Mono.<List<Sighting>>just(List.of()):aggregate(databaseClient.sql(
                        SELECT+" WHERE s.id IN (:ids) ORDER BY s.created_at DESC,s.id DESC,i.sort_order")
                        .bind("ids",ids).map(this::row).all())).toFuture();
    }
    private Flux<Long> insertImages(Sighting s){return Flux.fromIterable(s.images()).concatMap(i->databaseClient.sql("""
            INSERT INTO sighting_image(id,sighting_id,object_key,is_primary,sort_order)
            VALUES(:id,:sighting,:key,:primary,:sort)
            """).bind("id",i.id()).bind("sighting",s.id()).bind("key",i.objectKey())
            .bind("primary",i.primary()).bind("sort",i.sortOrder()).fetch().rowsUpdated());}
    private RowData row(io.r2dbc.spi.Row r,io.r2dbc.spi.RowMetadata m){UUID imageId=r.get("image_id",UUID.class);
        SightingImage image=imageId==null?null:new SightingImage(imageId,r.get("object_key",String.class),
                Boolean.TRUE.equals(r.get("is_primary",Boolean.class)),r.get("sort_order",Integer.class));
        return new RowData(r.get("id",UUID.class),r.get("reporter_id",UUID.class),Species.valueOf(r.get("species",String.class)),
                r.get("description",String.class),r.get("observed_at",Instant.class),r.get("latitude",Double.class),
                r.get("longitude",Double.class),r.get("neighborhood_id",UUID.class),SightingStatus.valueOf(r.get("status",String.class)),
                r.get("created_at",Instant.class),r.get("updated_at",Instant.class),r.get("version",Long.class),image);}
    private Mono<List<Sighting>> aggregate(Flux<RowData> rows){return rows.collectList().map(all->{Map<UUID,List<RowData>> groups=new LinkedHashMap<>();
        all.forEach(r->groups.computeIfAbsent(r.id(),x->new ArrayList<>()).add(r));return groups.values().stream().map(g->{RowData f=g.getFirst();
            return new Sighting(f.id(),f.reporter(),f.species(),f.description(),f.observed(),new GeoPoint(f.latitude(),f.longitude()),
                    f.neighborhood(),f.status(),g.stream().map(RowData::image).filter(Objects::nonNull).toList(),
                    f.created(),f.updated(),f.version());}).toList();});}
    private record RowData(UUID id,UUID reporter,Species species,String description,Instant observed,double latitude,
                           double longitude,UUID neighborhood,SightingStatus status,Instant created,Instant updated,
                           long version,SightingImage image){}
}
