package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QueryGeographicCatalogUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.GeographicCatalogRepository;
import java.util.*;
import java.util.concurrent.CompletionStage;

public final class QueryGeographicCatalogService implements QueryGeographicCatalogUseCase {
    private final GeographicCatalogRepository repository;

    public QueryGeographicCatalogService(GeographicCatalogRepository repository) { this.repository = repository; }

    @Override public CompletionStage<List<DepartmentView>> departments() {
        return repository.departments().thenApply(values -> values.stream()
                .map(value -> new DepartmentView(value.id(), value.name())).toList());
    }

    @Override public CompletionStage<List<CityView>> cities(UUID departmentId) {
        return repository.cities(Objects.requireNonNull(departmentId)).thenApply(values -> values.stream()
                .map(value -> new CityView(value.id(), value.departmentId(), value.name())).toList());
    }

    @Override public CompletionStage<List<NeighborhoodView>> neighborhoods(UUID cityId) {
        return repository.neighborhoods(Objects.requireNonNull(cityId)).thenApply(values -> values.stream()
                .map(value -> new NeighborhoodView(value.id(), value.cityId(), value.name())).toList());
    }
}
