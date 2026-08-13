package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;

import java.util.*;
import java.util.concurrent.*;

final class SightingImagePolicy {
    private SightingImagePolicy() { }

    static CompletionStage<List<String>> sanitize(ImageStoragePort storage, UUID ownerId, List<String> keys) {
        if (keys == null || keys.isEmpty() || keys.size() > 5 || keys.stream().distinct().count() != keys.size())
            return failed(new BusinessRuleViolation("A sighting requires 1 to 5 unique images"));
        String prefix = "sightings/staging/users/" + ownerId + "/";
        if (keys.stream().anyMatch(key -> key == null || !key.startsWith(prefix)))
            return failed(new BusinessRuleViolation("An image does not belong to the authenticated user"));
        List<String> sanitized = new ArrayList<>();
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (String key : keys) {
            chain = chain.thenCompose(ignored -> storage.sanitize(ownerId, ImageStoragePort.Category.SIGHTING, key)
                    .exceptionallyCompose(error -> failed(new BusinessRuleViolation("Uploaded image could not be validated")))
                    .thenCompose(object -> {
                        if (!valid(object)) return failed(new BusinessRuleViolation("Image must be a valid JPEG/PNG up to 8 MB"));
                        sanitized.add(object.objectKey());
                        return CompletableFuture.completedFuture(null);
                    }));
        }
        return chain.thenApply(ignored -> List.copyOf(sanitized)).exceptionallyCompose(error ->
                delete(storage, sanitized).thenCompose(ignored -> failed(unwrap(error))));
    }

    static CompletionStage<Void> delete(ImageStoragePort storage, List<String> keys) {
        var futures = keys.stream().map(storage::delete).map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    static boolean valid(ImageStoragePort.StoredObject object) {
        return object.contentLength() > 0 && object.contentLength() <= 8L * 1024 * 1024
                && List.of("image/jpeg", "image/png").contains(object.contentType())
                && object.checksumSha256() != null;
    }

    static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
    static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
