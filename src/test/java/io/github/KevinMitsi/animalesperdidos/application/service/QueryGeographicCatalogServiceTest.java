package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.out.GeographicCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryGeographicCatalogServiceTest {
    @Mock GeographicCatalogRepository repository;

    @Test void mapsCitiesWithoutExposingPersistenceRecords() {
        UUID department = UUID.randomUUID(); UUID city = UUID.randomUUID();
        when(repository.cities(department)).thenReturn(CompletableFuture.completedFuture(
                List.of(new GeographicCatalogRepository.CityEntry(city, department, "Armenia"))));
        var result = new QueryGeographicCatalogService(repository).cities(department).toCompletableFuture().join();
        assertEquals("Armenia", result.getFirst().name());
        assertEquals(department, result.getFirst().departmentId());
        verify(repository).cities(department);
    }

    @Test void resolvesNeighborhoodHierarchy() {
        UUID department = UUID.randomUUID(); UUID city = UUID.randomUUID(); UUID neighborhood = UUID.randomUUID();
        when(repository.findLocationByNeighborhoodId(neighborhood)).thenReturn(CompletableFuture.completedFuture(
                Optional.of(new GeographicCatalogRepository.NeighborhoodLocationEntry(department, "Quindio", city,
                        "Armenia", neighborhood, "La Castellana"))));

        var result = new QueryGeographicCatalogService(repository).resolveNeighborhood(neighborhood)
                .toCompletableFuture().join();

        assertEquals(department, result.departmentId());
        assertEquals(city, result.cityId());
        assertEquals(neighborhood, result.neighborhoodId());
        assertEquals("La Castellana", result.neighborhoodName());
        verify(repository).findLocationByNeighborhoodId(neighborhood);
    }

    @Test void failsWhenNeighborhoodDoesNotExist() {
        UUID neighborhood = UUID.randomUUID();
        when(repository.findLocationByNeighborhoodId(neighborhood)).thenReturn(
                CompletableFuture.completedFuture(Optional.empty()));

        var error = assertThrows(CompletionException.class, () -> new QueryGeographicCatalogService(repository)
                .resolveNeighborhood(neighborhood).toCompletableFuture().join());

        assertInstanceOf(ResourceNotFound.class, error.getCause());
    }
}
