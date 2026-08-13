package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PrepareSightingImageUploadUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrepareSightingImageUploadServiceTest {
    @Mock ImageStoragePort storage;

    @Test void delegatesAValidUploadToTheSightingCategory() {
        UUID reporter = UUID.randomUUID();
        String checksum = Base64.getEncoder().encodeToString(new byte[32]);
        var prepared = new ImageStoragePort.PreparedUpload("key", "url", "PUT", Map.of(), Instant.now());
        when(storage.prepareUpload(reporter, ImageStoragePort.Category.SIGHTING, "foto.jpg", "image/jpeg",
                1024, checksum, Duration.ofMinutes(10))).thenReturn(CompletableFuture.completedFuture(prepared));
        var result = new PrepareSightingImageUploadService(storage).prepare(
                new PrepareSightingImageUploadUseCase.Command(reporter, "foto.jpg", "image/jpeg", 1024, checksum))
                .toCompletableFuture().join();
        assertEquals("key", result.objectKey());
    }

    @Test void rejectsUnsupportedContentBeforeCallingStorage() {
        UUID reporter = UUID.randomUUID();
        String checksum = Base64.getEncoder().encodeToString(new byte[32]);
        assertThrows(BusinessRuleViolation.class, () -> new PrepareSightingImageUploadService(storage).prepare(
                new PrepareSightingImageUploadUseCase.Command(reporter, "foto.gif", "image/gif", 1024, checksum)));
        verifyNoInteractions(storage);
    }
}
