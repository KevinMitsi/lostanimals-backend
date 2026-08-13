package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ManageLostPetReportUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static io.github.KevinMitsi.animalesperdidos.application.service.AuthenticationServicesTest.completed;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageLostPetReportServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    @Mock LostPetReportRepository repository;
    @Mock ImageStoragePort storage;
    @Captor ArgumentCaptor<LostPetReport> reportCaptor;
    private ManageLostPetReportService service;

    @BeforeEach
    void setUp() {
        service = new ManageLostPetReportService(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void ownerCanEditAnActiveReport() {
        LostPetReport report = report(List.of("key-1"));
        when(repository.findById(report.id())).thenReturn(completed(Optional.of(report)));
        when(repository.update(any())).thenAnswer(invocation -> completed(invocation.getArgument(0)));
        ManageLostPetReportUseCase.Edit edit = new ManageLostPetReportUseCase.Edit("Luna nueva", Species.CAT,
                "Actualizada", NOW.minusSeconds(7200), 4.54, -75.68, UUID.randomUUID());

        service.edit(OWNER, report.id(), edit).toCompletableFuture().join();

        verify(repository).update(reportCaptor.capture());
        assertEquals("Luna nueva", reportCaptor.getValue().petName());
        assertEquals(Species.CAT, reportCaptor.getValue().species());
        assertEquals(NOW, reportCaptor.getValue().updatedAt());
    }

    @Test
    void anotherUserCannotModifyTheReport() {
        LostPetReport report = report(List.of("key-1"));
        when(repository.findById(report.id())).thenReturn(completed(Optional.of(report)));

        CompletionException error = assertThrows(CompletionException.class, () -> service.close(
                UUID.randomUUID(), report.id(), ReportStatus.REUNITED).toCompletableFuture().join());

        assertInstanceOf(ForbiddenOperation.class, error.getCause());
        verify(repository, never()).update(any());
    }

    @Test
    void addingAnImageRequiresAValidUploadedObject() {
        LostPetReport report = report(List.of("key-1"));
        String checksum = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        String key = "lost-pet-reports/staging/users/" + OWNER + "/new-" + "0".repeat(64) + ".jpg";
        String cleanKey = "lost-pet-reports/users/" + OWNER + "/clean-" + "0".repeat(64) + ".jpg";
        when(storage.sanitize(OWNER, ImageStoragePort.Category.LOST_PET_REPORT, key)).thenReturn(completed(
                new ImageStoragePort.StoredObject(cleanKey, "image/jpeg", 2000, checksum)));
        when(repository.findById(report.id())).thenReturn(completed(Optional.of(report)));
        when(repository.update(any())).thenAnswer(invocation -> completed(invocation.getArgument(0)));

        UUID imageId = service.addImage(OWNER, report.id(), key).toCompletableFuture().join();

        verify(repository).update(reportCaptor.capture());
        assertEquals(2, reportCaptor.getValue().images().size());
        assertEquals(imageId, reportCaptor.getValue().images().getLast().id());
        assertEquals(cleanKey, reportCaptor.getValue().images().getLast().objectKey());
    }

    @Test
    void removingTheLastImageIsRejected() {
        LostPetReport report = report(List.of("key-1"));
        when(repository.findById(report.id())).thenReturn(completed(Optional.of(report)));

        assertThrows(CompletionException.class, () -> service.removeImage(OWNER, report.id(),
                report.images().getFirst().id()).toCompletableFuture().join());

        verify(repository, never()).update(any());
        verifyNoInteractions(storage);
    }

    static LostPetReport report(List<String> keys) {
        return LostPetReport.create(UUID.randomUUID(), OWNER, "Luna", Species.DOG, "Collar rojo",
                NOW.minusSeconds(3600), new GeoPoint(4.5339, -75.6811), UUID.randomUUID(), keys,
                NOW.minusSeconds(60));
    }
}
