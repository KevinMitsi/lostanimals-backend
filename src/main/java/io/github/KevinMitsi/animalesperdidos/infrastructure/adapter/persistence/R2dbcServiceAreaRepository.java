package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ServiceAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository @RequiredArgsConstructor
public class R2dbcServiceAreaRepository implements ServiceAreaRepository {
    private final DatabaseClient databaseClient;
    @Override public CompletionStage<Boolean> isNeighborhoodEnabled(UUID neighborhoodId) {
        return databaseClient.sql("""
                SELECT EXISTS(SELECT 1 FROM neighborhood n JOIN service_area a ON a.city_id=n.city_id
                  WHERE n.id=:id AND a.enabled) enabled
                """).bind("id", neighborhoodId).map((row,metadata) -> Boolean.TRUE.equals(row.get("enabled",Boolean.class)))
                .one().defaultIfEmpty(false).toFuture();
    }
    @Override public CompletionStage<List<AreaEntry>> list() {
        return databaseClient.sql("""
                SELECT c.id city_id,c.name city_name,d.id department_id,d.name department_name,
                  COALESCE(a.enabled,false) enabled
                FROM city c JOIN department d ON d.id=c.department_id
                LEFT JOIN service_area a ON a.city_id=c.id ORDER BY d.name,c.name,c.id
                """).map((row,metadata) -> new AreaEntry(row.get("city_id",UUID.class),row.get("city_name",String.class),
                row.get("department_id",UUID.class),row.get("department_name",String.class),
                Boolean.TRUE.equals(row.get("enabled",Boolean.class)))).all().collectList().toFuture();
    }
    @Override public CompletionStage<Void> setEnabled(UUID cityId, boolean enabled, UUID actorId, Instant now) {
        return databaseClient.sql("""
                INSERT INTO service_area(city_id,enabled,updated_by,updated_at) VALUES(:city,:enabled,:actor,:now)
                ON CONFLICT(city_id) DO UPDATE SET enabled=excluded.enabled,updated_by=excluded.updated_by,
                  updated_at=excluded.updated_at
                """).bind("city",cityId).bind("enabled",enabled).bind("actor",actorId).bind("now",now)
                .fetch().rowsUpdated().flatMap(rows -> rows == 1 ? Mono.just(rows) : Mono.error(new ResourceNotFound("City")))
                .then().toFuture();
    }
}
