package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PrepareReportImageUploadUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Map;
import java.util.UUID;

import static io.github.KevinMitsi.animalesperdidos.application.service.AuthenticationServicesTest.completed;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrepareReportImageUploadServiceTest {
    @Mock ImageStoragePort storage;

    @Test
    void preparesAtenMinutePutUrl() {
        UUID owner = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(600);
        String checksum = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        when(storage.prepareUpload(eq(owner), eq(ImageStoragePort.Category.LOST_PET_REPORT), eq("luna.jpg"), eq("image/jpeg"), eq(1024L), eq(checksum), eq(Duration.ofMinutes(10))))
                .thenReturn(completed(new ImageStoragePort.PreparedUpload("key", "https://s3/upload", "PUT",
                        Map.of("content-type", "image/jpeg"), expiry)));

        var result = new PrepareReportImageUploadService(storage).prepare(
                new PrepareReportImageUploadUseCase.Command(owner, "luna.jpg", "image/jpeg", 1024, checksum))
                .toCompletableFuture().join();

        assertEquals("key", result.objectKey());
        assertEquals("PUT", result.method());
    }

    @Test
    void rejectsUnsupportedMediaBeforeCallingStorage() {
        PrepareReportImageUploadService service = new PrepareReportImageUploadService(storage);
        assertThrows(BusinessRuleViolation.class, () -> service.prepare(
                new PrepareReportImageUploadUseCase.Command(UUID.randomUUID(), "x.svg", "image/svg+xml", 100,
                        java.util.Base64.getEncoder().encodeToString(new byte[32]))));
        verifyNoInteractions(storage);
    }
}
