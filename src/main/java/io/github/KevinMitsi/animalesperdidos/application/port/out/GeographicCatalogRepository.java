package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.*;
import java.util.concurrent.CompletionStage;

public interface GeographicCatalogRepository {
    CompletionStage<List<DepartmentEntry>> departments();
    CompletionStage<List<CityEntry>> cities(UUID departmentId);
    CompletionStage<List<NeighborhoodEntry>> neighborhoods(UUID cityId);
    CompletionStage<Optional<NeighborhoodLocationEntry>> findLocationByNeighborhoodId(UUID neighborhoodId);

    record DepartmentEntry(UUID id, String name) { }
    record CityEntry(UUID id, UUID departmentId, String name) { }
    record NeighborhoodEntry(UUID id, UUID cityId, String name) { }
    record NeighborhoodLocationEntry(UUID departmentId, String departmentName, UUID cityId, String cityName,
                                     UUID neighborhoodId, String neighborhoodName) { }
}
