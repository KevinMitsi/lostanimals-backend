package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.KevinMitsi.animalesperdidos.domain.model.GeoPoint;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ReportLostPetService implements ReportLostPetUseCase {
    private static final int DAILY_REPORT_LIMIT = 3;
    private static final Duration DUPLICATE_WINDOW = Duration.ofHours(24);

    private final LostPetReportRepository repository;
    private final ImageStoragePort imageStorage;
    private final NotificationPort notification;
    private final Clock clock;

    public ReportLostPetService(LostPetReportRepository repository, ImageStoragePort imageStorage,
                                NotificationPort notification, Clock clock) {
        this.repository = repository;
        this.imageStorage = imageStorage;
        this.notification = notification;
        this.clock = clock;
    }

    @Override
    public CompletionStage<Result> report(Command command) {
        Instant now = clock.instant();
        validateImageKeys(command.imageKeys());
        if (command.disappearedAt().isAfter(now)) {
            throw new BusinessRuleViolation("Disappearance time cannot be in the future");
        }
        new GeoPoint(command.latitude(), command.longitude());
        CompletionStage<Boolean> duplicate = repository.existsActiveDuplicate(command.ownerId(), command.species(),
                command.petName(), now.minus(DUPLICATE_WINDOW));
        CompletionStage<Long> dailyCount = repository.countCreatedByOwnerSince(command.ownerId(),
                now.minus(Duration.ofDays(1)));

        return duplicate.thenCombine(dailyCount, Checks::new)
                .thenCompose(this::enforcePublicationRules)
                .thenCompose(ignored -> sanitizeUploadedImages(command.ownerId(), command.imageKeys()))
                .thenCompose(keys -> persist(command, keys, now));
    }

    private CompletionStage<Result> persist(Command command, List<String> sanitizedKeys, Instant now) {
        UUID reportId = UUID.randomUUID();
        LostPetReport report = LostPetReport.create(reportId, command.ownerId(), command.petName(),
                command.species(), command.description(), command.disappearedAt(),
                new GeoPoint(command.latitude(), command.longitude()), command.neighborhoodId(),
                sanitizedKeys, now);
        return repository.save(report)
                .exceptionallyCompose(error -> deleteImages(sanitizedKeys).thenCompose(ignored -> failed(error)))
                .thenCompose(saved -> notification.reportCreated(saved)
                .exceptionally(ignored -> null)
                .thenApply(ignored -> new Result(saved.id())));
    }

    private CompletionStage<Void> enforcePublicationRules(Checks checks) {
        if (checks.duplicate()) {
            return failed(new BusinessRuleViolation("An active report already exists for this pet in the last 24 hours"));
        }
        if (checks.dailyCount() >= DAILY_REPORT_LIMIT) {
            return failed(new BusinessRuleViolation("Daily report limit reached"));
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void validateImageKeys(List<String> keys) {
        if (keys == null || keys.isEmpty() || keys.size() > 5) {
            throw new BusinessRuleViolation("A report must contain between 1 and 5 images");
        }
        if (keys.stream().anyMatch(key -> key == null || key.isBlank()) || keys.stream().distinct().count() != keys.size()) {
            throw new BusinessRuleViolation("Image keys must be unique and non-empty");
        }
    }

    private CompletionStage<List<String>> sanitizeUploadedImages(UUID ownerId, List<String> keys) {
        String requiredPrefix = "lost-pet-reports/staging/users/" + ownerId + "/";
        if (keys.stream().anyMatch(key -> !key.startsWith(requiredPrefix))) {
            return failed(new BusinessRuleViolation("An image does not belong to the authenticated user"));
        }
        List<String> sanitized = new java.util.ArrayList<>();
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (String key : keys) {
            chain = chain.thenCompose(ignored -> imageStorage.sanitize(ownerId,
                            ImageStoragePort.Category.LOST_PET_REPORT, key)
                    .exceptionallyCompose(error -> failed(new BusinessRuleViolation("Uploaded image could not be validated")))
                    .thenCompose(object -> {
                if (!validImage(object)) {
                    return failed(new BusinessRuleViolation("Uploaded images must be valid JPEG or PNG files and at most 8 MB"));
                }
                sanitized.add(object.objectKey());
                return CompletableFuture.completedFuture(null);
            }));
        }
        return chain.thenApply(ignored -> List.copyOf(sanitized))
                .exceptionallyCompose(error -> deleteImages(List.copyOf(sanitized))
                        .thenCompose(ignored -> failed(error instanceof java.util.concurrent.CompletionException
                                && error.getCause() != null ? error.getCause() : error)));
    }

    private CompletionStage<Void> deleteImages(List<String> keys) {
        List<CompletionStage<Void>> deletes = keys.stream().map(imageStorage::delete).toList();
        return CompletableFuture.allOf(deletes.stream().map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new));
    }

    private static boolean validImage(ImageStoragePort.StoredObject object) {
        return object.contentLength() > 0 && object.contentLength() <= 8L * 1024 * 1024
                && List.of("image/jpeg", "image/png").contains(object.contentType())
                && object.checksumSha256() != null;
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }

    private record Checks(boolean duplicate, long dailyCount) {
    }
}
