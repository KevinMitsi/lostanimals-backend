package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.*;
import java.util.concurrent.CompletionStage;

public interface GeographicCatalogRepository {
    CompletionStage<List<DepartmentEntry>> departments();
    CompletionStage<List<CityEntry>> cities(UUID departmentId);
    CompletionStage<List<NeighborhoodEntry>> neighborhoods(UUID cityId);

    record DepartmentEntry(UUID id, String name) { }
    record CityEntry(UUID id, UUID departmentId, String name) { }
    record NeighborhoodEntry(UUID id, UUID cityId, String name) { }
}
