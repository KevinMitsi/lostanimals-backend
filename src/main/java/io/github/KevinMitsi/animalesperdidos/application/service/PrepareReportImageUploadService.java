package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PrepareReportImageUploadUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public final class PrepareReportImageUploadService implements PrepareReportImageUploadUseCase {
    private static final long MAX_SIZE = 8L * 1024 * 1024;
    private static final Duration VALIDITY = Duration.ofMinutes(10);
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png");
    private final ImageStoragePort storage;

    public PrepareReportImageUploadService(ImageStoragePort storage) { this.storage = storage; }

    @Override
    public CompletionStage<Result> prepare(Command command) {
        if (!ALLOWED_TYPES.contains(command.contentType())) {
            throw new BusinessRuleViolation("Only JPEG and PNG images are supported");
        }
        if (command.contentLength() <= 0 || command.contentLength() > MAX_SIZE) {
            throw new BusinessRuleViolation("Image size must be between 1 byte and 8 MB");
        }
        try {
            if (java.util.Base64.getDecoder().decode(command.checksumSha256()).length != 32) {
                throw new BusinessRuleViolation("Checksum must be SHA-256 encoded as Base64");
            }
        } catch (IllegalArgumentException invalidBase64) {
            throw new BusinessRuleViolation("Checksum must be SHA-256 encoded as Base64");
        }
        return storage.prepareUpload(command.ownerId(), ImageStoragePort.Category.LOST_PET_REPORT,
                        command.fileName(), command.contentType(),
                        command.contentLength(), command.checksumSha256(), VALIDITY)
                .thenApply(upload -> new Result(upload.objectKey(), upload.uploadUrl(), upload.method(),
                        upload.requiredHeaders(), upload.expiresAt()));
    }
}
