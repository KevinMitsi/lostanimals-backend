package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.port.out.GeographicCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcGeographicCatalogRepository implements GeographicCatalogRepository {
    private final DatabaseClient databaseClient;

    @Override public CompletionStage<List<DepartmentEntry>> departments() {
        return databaseClient.sql("SELECT id,name FROM department ORDER BY name,id")
                .map((row, metadata) -> new DepartmentEntry(row.get("id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }

    @Override public CompletionStage<List<CityEntry>> cities(UUID departmentId) {
        return databaseClient.sql("SELECT id,department_id,name FROM city WHERE department_id=:id ORDER BY name,id")
                .bind("id", departmentId).map((row, metadata) -> new CityEntry(row.get("id", UUID.class),
                        row.get("department_id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }

    @Override public CompletionStage<List<NeighborhoodEntry>> neighborhoods(UUID cityId) {
        return databaseClient.sql("SELECT id,city_id,name FROM neighborhood WHERE city_id=:id ORDER BY name,id")
                .bind("id", cityId).map((row, metadata) -> new NeighborhoodEntry(row.get("id", UUID.class),
                        row.get("city_id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }
}
