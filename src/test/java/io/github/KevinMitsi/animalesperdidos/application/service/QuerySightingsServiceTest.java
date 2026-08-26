package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QuerySightingsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuerySightingsServiceTest {
    @Mock SightingRepository repository;
    @Mock ImageStoragePort storage;

    @Test void publicDetailUsesApproximateCoordinatesAndSignedImageUrl() {
        Sighting sighting = sighting();
        when(repository.findById(sighting.id())).thenReturn(done(Optional.of(sighting)));
        when(storage.createDownloadUrl(anyString(), eq(Duration.ofMinutes(15)))).thenReturn(done("https://signed.example/image"));
        var view = new QuerySightingsService(repository, storage).getPublic(sighting.id()).toCompletableFuture().join();
        assertEquals(4.534, view.latitude());
        assertEquals(-75.681, view.longitude());
        assertEquals("https://signed.example/image", view.images().getFirst().url());
    }

    @Test void mineKeepsExactCoordinates() {
        Sighting sighting = sighting();
        when(repository.search(any())).thenReturn(done(List.of(sighting)));
        when(storage.createDownloadUrl(anyString(), any())).thenReturn(done("url"));
        var page = new QuerySightingsService(repository, storage).mine(sighting.reporterId(),
                new QuerySightingsUseCase.Search(null, null, null, null, null, null, null,
                        null, null, null, null, 20)).toCompletableFuture().join();
        assertEquals(4.53391, page.items().getFirst().latitude());
    }

    @Test void passesGeospatialAndTerritorialFiltersToRepository() {
        ArgumentCaptor<SightingRepository.SearchCriteria> captor = ArgumentCaptor.forClass(SightingRepository.SearchCriteria.class);
        when(repository.search(any())).thenReturn(done(List.of()));
        String department = "63"; String municipality = "63001";
        var search = new QuerySightingsUseCase.Search(Species.CAT, department, municipality, null, SightingStatus.ACTIVE,
                Instant.parse("2026-08-01T00:00:00Z"), null, 4.53, -75.68, 5000d, null, 10);
        new QuerySightingsService(repository, storage).searchPublic(search).toCompletableFuture().join();
        verify(repository).search(captor.capture());
        assertEquals(department, captor.getValue().departmentCode());
        assertEquals(municipality, captor.getValue().municipalityCode());
        assertEquals(5000d, captor.getValue().area().radiusMeters());
        assertFalse(captor.getValue().exactLocation());
    }

    @Test void publicMapSearchProvidesPopupImageDateAndOnlyApproximateCoordinates() {
        Sighting sighting = sighting();
        when(repository.search(any())).thenReturn(done(List.of(sighting)));
        when(storage.createDownloadUrl(anyString(), eq(Duration.ofMinutes(15))))
                .thenReturn(done("https://signed.example/map-image"));

        var page = new QuerySightingsService(repository, storage).searchPublic(
                new QuerySightingsUseCase.Search(null, null, null, null, SightingStatus.ACTIVE,
                        null, null, 4.71, -74.07, 5_000d, null, 50)).toCompletableFuture().join();

        var mapItem = page.items().getFirst();
        assertEquals(sighting.id(), mapItem.id());
        assertEquals(sighting.createdAt(), mapItem.createdAt());
        assertEquals("https://signed.example/map-image", mapItem.images().getFirst().url());
        assertEquals(4.534, mapItem.latitude());
        assertEquals(-75.681, mapItem.longitude());
    }

    private static Sighting sighting() {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");
        return Sighting.create(UUID.randomUUID(), UUID.randomUUID(), Species.DOG, "Descripción", now.minusSeconds(60),
                new GeoPoint(4.53391, -75.68114), new AdministrativeLocation("63","63001","Granada"),
                List.of("sightings/users/u/1.jpg"), now);
    }
    private static <T> CompletableFuture<T> done(T value) { return CompletableFuture.completedFuture(value); }
}
