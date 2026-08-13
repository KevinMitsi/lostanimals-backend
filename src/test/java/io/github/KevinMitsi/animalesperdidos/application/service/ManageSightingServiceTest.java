package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ManageSightingUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageSightingServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    @Mock SightingRepository repository;
    @Mock ImageStoragePort storage;
    private ManageSightingService service;

    @BeforeEach void setUp() { service = new ManageSightingService(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test void ownerCanCloseTheSighting() {
        Sighting sighting = sighting();
        when(repository.findById(sighting.id())).thenReturn(done(Optional.of(sighting)));
        when(repository.update(any())).thenAnswer(invocation -> done(invocation.getArgument(0)));
        service.close(OWNER, sighting.id()).toCompletableFuture().join();
        verify(repository).update(argThat(value -> value.status() == SightingStatus.CLOSED));
    }

    @Test void anotherUserCannotEditIt() {
        Sighting sighting = sighting();
        when(repository.findById(sighting.id())).thenReturn(done(Optional.of(sighting)));
        var edit = new ManageSightingUseCase.Edit(Species.CAT, "Descripción", NOW.minusSeconds(20),
                4.53, -75.68, sighting.neighborhoodId());
        CompletionException error = assertThrows(CompletionException.class,
                () -> service.edit(UUID.randomUUID(), sighting.id(), edit).toCompletableFuture().join());
        assertInstanceOf(ForbiddenOperation.class, error.getCause());
        verify(repository, never()).update(any());
    }

    private static Sighting sighting() {
        return Sighting.create(UUID.randomUUID(), OWNER, Species.DOG, "Descripción", NOW.minusSeconds(100),
                new GeoPoint(4.5339, -75.6811), UUID.randomUUID(), List.of("sightings/users/u/1.jpg"), NOW.minusSeconds(10));
    }
    private static <T> CompletableFuture<T> done(T value) { return CompletableFuture.completedFuture(value); }
}
