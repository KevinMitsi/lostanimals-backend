package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.CreateSightingUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSightingServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final UUID REPORTER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String STAGING = "sightings/staging/users/" + REPORTER + "/image.jpg";
    private static final String CLEAN = "sightings/users/" + REPORTER + "/image.jpg";
    @Mock SightingRepository repository;
    @Mock ImageStoragePort storage;
    private CreateSightingService service;

    @BeforeEach void setUp() { service = new CreateSightingService(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test void warnsAboutNearbyDuplicateWithoutPublishingOrTouchingStorage() {
        UUID duplicate = UUID.randomUUID();
        when(repository.findNearbyDuplicate(any(), any(), any(), any(), eq(50d))).thenReturn(done(Optional.of(
                new SightingRepository.DuplicateCandidate(duplicate, 12.4, NOW.minusSeconds(60)))));
        var result = service.create(command(false)).toCompletableFuture().join();
        assertFalse(result.created());
        assertEquals(duplicate, result.warning().existingSightingId());
        verifyNoInteractions(storage);
        verify(repository, never()).save(any());
    }

    @Test void explicitConfirmationSanitizesAndPublishes() {
        when(repository.findNearbyDuplicate(any(), any(), any(), any(), eq(50d))).thenReturn(done(Optional.of(
                new SightingRepository.DuplicateCandidate(UUID.randomUUID(), 12.4, NOW.minusSeconds(60)))));
        when(storage.sanitize(REPORTER, ImageStoragePort.Category.SIGHTING, STAGING)).thenReturn(done(
                new ImageStoragePort.StoredObject(CLEAN, "image/jpeg", 1024, "checksum")));
        when(repository.save(any())).thenAnswer(invocation -> done(invocation.getArgument(0)));
        var result = service.create(command(true)).toCompletableFuture().join();
        assertTrue(result.created());
        assertNotNull(result.sightingId());
        verify(repository).save(argThat(s -> s.images().getFirst().objectKey().equals(CLEAN)));
    }

    private static CreateSightingUseCase.Command command(boolean confirm) {
        return new CreateSightingUseCase.Command(REPORTER, Species.DOG, "Visto cerca al parque", NOW.minusSeconds(300),
                4.5339, -75.6811, UUID.randomUUID(), List.of(STAGING), confirm);
    }
    private static <T> CompletableFuture<T> done(T value) { return CompletableFuture.completedFuture(value); }
}
