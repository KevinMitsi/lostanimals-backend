package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PrepareSightingImageUploadUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionStage;

public final class PrepareSightingImageUploadService implements PrepareSightingImageUploadUseCase {
    private final ImageStoragePort storage;
    public PrepareSightingImageUploadService(ImageStoragePort storage) { this.storage = storage; }
    @Override public CompletionStage<Result> prepare(Command command) {
        if (!java.util.List.of("image/jpeg", "image/png").contains(command.contentType()))
            throw new BusinessRuleViolation("Only JPEG and PNG images are supported");
        if (command.contentLength() <= 0 || command.contentLength() > 8L * 1024 * 1024)
            throw new BusinessRuleViolation("Image size must be between 1 byte and 8 MB");
        try { if (Base64.getDecoder().decode(command.checksumSha256()).length != 32) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException error) { throw new BusinessRuleViolation("Checksum must be Base64 SHA-256"); }
        return storage.prepareUpload(command.reporterId(), ImageStoragePort.Category.SIGHTING,
                command.fileName(), command.contentType(), command.contentLength(), command.checksumSha256(),
                Duration.ofMinutes(10)).thenApply(upload -> new Result(upload.objectKey(), upload.uploadUrl(),
                upload.method(), upload.requiredHeaders(), upload.expiresAt()));
    }
}
