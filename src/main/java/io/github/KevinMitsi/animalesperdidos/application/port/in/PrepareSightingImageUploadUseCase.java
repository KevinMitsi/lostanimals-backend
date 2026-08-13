package io.github.KevinMitsi.animalesperdidos.application.port.in;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface PrepareSightingImageUploadUseCase {
    CompletionStage<Result> prepare(Command command);
    record Command(UUID reporterId, String fileName, String contentType, long contentLength, String checksumSha256) { }
    record Result(String objectKey, String uploadUrl, String method, Map<String,String> requiredHeaders, Instant expiresAt) { }
}
