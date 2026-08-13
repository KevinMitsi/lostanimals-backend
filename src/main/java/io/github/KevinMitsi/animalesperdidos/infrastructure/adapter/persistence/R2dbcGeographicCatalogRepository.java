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
        return databaseClient.sql("""
                SELECT DISTINCT d.id,d.name FROM department d JOIN city c ON c.department_id=d.id
                JOIN service_area a ON a.city_id=c.id AND a.enabled ORDER BY d.name,d.id
                """)
                .map((row, metadata) -> new DepartmentEntry(row.get("id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }

    @Override public CompletionStage<List<CityEntry>> cities(UUID departmentId) {
        return databaseClient.sql("""
                SELECT c.id,c.department_id,c.name FROM city c JOIN service_area a ON a.city_id=c.id AND a.enabled
                WHERE c.department_id=:id ORDER BY c.name,c.id
                """)
                .bind("id", departmentId).map((row, metadata) -> new CityEntry(row.get("id", UUID.class),
                        row.get("department_id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }

    @Override public CompletionStage<List<NeighborhoodEntry>> neighborhoods(UUID cityId) {
        return databaseClient.sql("""
                SELECT n.id,n.city_id,n.name FROM neighborhood n JOIN service_area a ON a.city_id=n.city_id AND a.enabled
                WHERE n.city_id=:id ORDER BY n.name,n.id
                """)
                .bind("id", cityId).map((row, metadata) -> new NeighborhoodEntry(row.get("id", UUID.class),
                        row.get("city_id", UUID.class), row.get("name", String.class)))
                .all().collectList().toFuture();
    }
}
