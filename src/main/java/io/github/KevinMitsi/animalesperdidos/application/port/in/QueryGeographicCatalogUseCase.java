package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.util.*;
import java.util.concurrent.CompletionStage;

public interface QueryGeographicCatalogUseCase {
    CompletionStage<List<DepartmentView>> departments();
    CompletionStage<List<CityView>> cities(UUID departmentId);
    CompletionStage<List<NeighborhoodView>> neighborhoods(UUID cityId);
    CompletionStage<NeighborhoodLocationView> resolveNeighborhood(UUID neighborhoodId);

    record DepartmentView(UUID id, String name) { }
    record CityView(UUID id, UUID departmentId, String name) { }
    record NeighborhoodView(UUID id, UUID cityId, String name) { }
    record NeighborhoodLocationView(UUID departmentId, String departmentName, UUID cityId, String cityName,
                                    UUID neighborhoodId, String neighborhoodName) { }
}
