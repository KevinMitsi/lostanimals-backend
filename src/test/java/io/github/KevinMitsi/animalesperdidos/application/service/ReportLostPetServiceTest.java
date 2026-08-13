package io.github.kevinmitsi.animalesperdidos.application.service;

import io.github.kevinmitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.kevinmitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.kevinmitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.kevinmitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.kevinmitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;
import io.github.kevinmitsi.animalesperdidos.domain.model.Species;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class ReportLostPetServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void storesTheImageAndPersistsTheReport() {
        FakeRepository repository = new FakeRepository();
        ReportLostPetService service = service(repository);

        ReportLostPetUseCase.Result result = service.report(command()).toCompletableFuture().join();

        assertNotNull(result.reportId());
        assertNotNull(repository.saved);
        assertEquals(of("lost-pet-reports/image.jpg"), repository.saved.imageKeys());
        assertEquals("Luna", repository.saved.petName());
    }

    @Test
    void rejectsAnActiveDuplicateBeforeUploading() {
        FakeRepository repository = new FakeRepository();
        repository.duplicate = true;

        CompletionException error = assertThrows(CompletionException.class,
                () -> service(repository).report(command()).toCompletableFuture().join());

        assertInstanceOf(BusinessRuleViolation.class, error.getCause());
        assertNull(repository.saved);
    }

    private static ReportLostPetService service(FakeRepository repository) {
        ImageStoragePort storage = new ImageStoragePort() {
            public CompletionStage<String> store(UUID id, String name, String type, byte[] content) {
                return CompletableFuture.completedFuture("lost-pet-reports/image.jpg");
            }

            public CompletionStage<Void> delete(String key) {
                return CompletableFuture.completedFuture(null);
            }
        };
        NotificationPort notification = report -> CompletableFuture.completedFuture(null);
        return new ReportLostPetService(repository, storage, notification, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ReportLostPetUseCase.Command command() {
        return new ReportLostPetUseCase.Command(UUID.randomUUID(), "Luna", Species.DOG, "Collar rojo",
                NOW.minusSeconds(3600), 4.5339, -75.6811, UUID.randomUUID(),
                of(new ReportLostPetUseCase.Image("luna.jpg", "image/jpeg", new byte[]{1, 2, 3})));
    }

    private static final class FakeRepository implements LostPetReportRepository {
        private boolean duplicate;
        private LostPetReport saved;

        public CompletionStage<Boolean> existsActiveDuplicate(UUID ownerId, Species species, String name, Instant since) {
            return CompletableFuture.completedFuture(duplicate);
        }

        public CompletionStage<Long> countCreatedByOwnerSince(UUID ownerId, Instant since) {
            return CompletableFuture.completedFuture(0L);
        }

        public CompletionStage<LostPetReport> save(LostPetReport report) {
            saved = report;
            return CompletableFuture.completedFuture(report);
        }
    }
}
