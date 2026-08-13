package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ImageStoragePort {

    CompletionStage<String> store(UUID reportId, String fileName, String contentType, byte[] content);

    CompletionStage<Void> delete(String objectKey);

    CompletionStage<PreparedUpload> prepareUpload(UUID ownerId, String fileName, String contentType,
                                                   long contentLength, String checksumSha256, Duration validity);

    CompletionStage<StoredObject> inspect(String objectKey);

    CompletionStage<StoredObject> sanitize(UUID ownerId, String stagingObjectKey);

    CompletionStage<String> createDownloadUrl(String objectKey, Duration validity);

    record PreparedUpload(String objectKey, String uploadUrl, String method,
                          Map<String, String> requiredHeaders, Instant expiresAt) { }

    record StoredObject(String objectKey, String contentType, long contentLength, String checksumSha256) { }
}
