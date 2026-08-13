package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QuerySightingsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
                new QuerySightingsUseCase.Search(null, null, null, null, null, 20)).toCompletableFuture().join();
        assertEquals(4.53391, page.items().getFirst().latitude());
    }

    private static Sighting sighting() {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");
        return Sighting.create(UUID.randomUUID(), UUID.randomUUID(), Species.DOG, "Descripción", now.minusSeconds(60),
                new GeoPoint(4.53391, -75.68114), UUID.randomUUID(), List.of("sightings/users/u/1.jpg"), now);
    }
    private static <T> CompletableFuture<T> done(T value) { return CompletableFuture.completedFuture(value); }
}
