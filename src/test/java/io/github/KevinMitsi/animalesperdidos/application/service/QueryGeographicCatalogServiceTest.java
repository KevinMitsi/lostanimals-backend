package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.out.GeographicCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
}
