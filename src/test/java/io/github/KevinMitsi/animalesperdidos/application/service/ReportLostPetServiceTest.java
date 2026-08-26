package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ServiceAreaRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import io.github.KevinMitsi.animalesperdidos.domain.model.AdministrativeLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportLostPetServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String CHECKSUM = java.util.Base64.getEncoder().encodeToString(new byte[32]);
    private static final String IMAGE_KEY = "lost-pet-reports/staging/users/" + OWNER_ID + "/image-"
            + "0".repeat(64) + ".jpg";
    private static final String SANITIZED_KEY = "lost-pet-reports/users/" + OWNER_ID + "/clean-"
            + "0".repeat(64) + ".jpg";

    @Mock LostPetReportRepository repository;
    @Mock ImageStoragePort storage;
    @Mock NotificationPort notification;
    @Mock ServiceAreaRepository serviceAreas;
    @Captor ArgumentCaptor<LostPetReport> reportCaptor;
    private ReportLostPetService service;

    @BeforeEach
    void setUp() {
        lenient().when(serviceAreas.isMunicipalityEnabled(any())).thenReturn(completed(true));
        service = new ReportLostPetService(repository, storage, notification,
                Clock.fixed(NOW, ZoneOffset.UTC), serviceAreas);
    }

    @Test
    void storesTheImageAndPersistsTheReport() {
        when(repository.existsActiveDuplicate(any(), any(), anyString(), any())).thenReturn(completed(false));
        when(repository.countCreatedByOwnerSince(any(), any())).thenReturn(completed(0L));
        when(storage.sanitize(OWNER_ID, ImageStoragePort.Category.LOST_PET_REPORT, IMAGE_KEY)).thenReturn(completed(
                new ImageStoragePort.StoredObject(SANITIZED_KEY, "image/jpeg", 1024, CHECKSUM)));
        when(repository.save(any())).thenAnswer(invocation -> completed(invocation.getArgument(0)));
        when(notification.reportCreated(any())).thenReturn(completed(null));

        ReportLostPetUseCase.Result result = service.report(command()).toCompletableFuture().join();

        verify(repository).save(reportCaptor.capture());
        assertNotNull(result.reportId());
        assertEquals(of(SANITIZED_KEY), reportCaptor.getValue().images().stream().map(image -> image.objectKey()).toList());
        assertEquals("Luna", reportCaptor.getValue().petName());
    }

    @Test
    void rejectsAnActiveDuplicateBeforeUploading() {
        when(repository.existsActiveDuplicate(any(), any(), anyString(), any())).thenReturn(completed(true));
        when(repository.countCreatedByOwnerSince(any(), any())).thenReturn(completed(0L));

        CompletionException error = assertThrows(CompletionException.class,
                () -> service.report(command()).toCompletableFuture().join());

        assertInstanceOf(BusinessRuleViolation.class, error.getCause());
        verifyNoInteractions(storage, notification);
        verify(repository, never()).save(any());
    }

    private static ReportLostPetUseCase.Command command() {
        return new ReportLostPetUseCase.Command(OWNER_ID, "Luna", Species.DOG, "Collar rojo",
                NOW.minusSeconds(3600), 4.5339, -75.6811,
                new AdministrativeLocation("63","63001","Granada"),
                of(IMAGE_KEY));
    }

    private static <T> CompletableFuture<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }
}
