package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.port.out.ServiceAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository @RequiredArgsConstructor
public class R2dbcServiceAreaRepository implements ServiceAreaRepository {
    private final DatabaseClient databaseClient;
    @Override public CompletionStage<Boolean> isMunicipalityEnabled(String municipalityCode) {
        return databaseClient.sql("""
                SELECT COALESCE((SELECT enabled FROM service_area WHERE municipality_code=:code),true) enabled
                """).bind("code", municipalityCode)
                .map((row,metadata) -> Boolean.TRUE.equals(row.get("enabled",Boolean.class)))
                .one().defaultIfEmpty(true).toFuture();
    }
    @Override public CompletionStage<List<AreaEntry>> list() {
        return databaseClient.sql("""
                SELECT municipality_code,enabled FROM service_area ORDER BY municipality_code
                """).map((row,metadata) -> new AreaEntry(row.get("municipality_code",String.class),
                Boolean.TRUE.equals(row.get("enabled",Boolean.class)))).all().collectList().toFuture();
    }
    @Override public CompletionStage<Void> setEnabled(String municipalityCode, boolean enabled, UUID actorId, Instant now) {
        return databaseClient.sql("""
                INSERT INTO service_area(municipality_code,enabled,updated_by,updated_at) VALUES(:code,:enabled,:actor,:now)
                ON CONFLICT(municipality_code) DO UPDATE SET enabled=excluded.enabled,updated_by=excluded.updated_by,
                  updated_at=excluded.updated_at
                """).bind("code",municipalityCode).bind("enabled",enabled).bind("actor",actorId).bind("now",now)
                .fetch().rowsUpdated().then().toFuture();
    }
}
