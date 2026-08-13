package io.github.kevinmitsi.animalesperdidos.application.service;

import io.github.kevinmitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.kevinmitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.kevinmitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.kevinmitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.kevinmitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.kevinmitsi.animalesperdidos.domain.model.GeoPoint;
import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
        validateImages(command.images());
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
                .thenCompose(ignored -> storeAndPersist(command, now));
    }

    private CompletionStage<Result> storeAndPersist(Command command, Instant now) {
        UUID reportId = UUID.randomUUID();
        List<CompletionStage<String>> uploads = command.images().stream()
                .map(image -> imageStorage.store(reportId, image.fileName(), image.contentType(), image.content()))
                .toList();

        return sequence(uploads).thenCompose(keys -> {
            LostPetReport report = LostPetReport.create(reportId, command.ownerId(), command.petName(),
                    command.species(), command.description(), command.disappearedAt(),
                    new GeoPoint(command.latitude(), command.longitude()), command.neighborhoodId(), keys, now);
            CompletionStage<LostPetReport> persisted = repository.save(report)
                    .exceptionallyCompose(error -> deleteUploadedImages(keys).thenCompose(ignored -> failed(error)));
            return persisted.thenCompose(saved -> notification.reportCreated(saved)
                    .exceptionally(ignored -> null)
                    .thenApply(ignored -> new Result(saved.id())));
        });
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

    private static void validateImages(List<Image> images) {
        if (images == null || images.isEmpty() || images.size() > 5) {
            throw new BusinessRuleViolation("A report must contain between 1 and 5 images");
        }
        if (images.stream().anyMatch(image -> image.content().length == 0 || !image.contentType().startsWith("image/"))) {
            throw new BusinessRuleViolation("Only non-empty image files are accepted");
        }
    }

    private CompletionStage<Void> deleteUploadedImages(List<String> keys) {
        return sequence(keys.stream().map(imageStorage::delete).toList()).thenApply(ignored -> null);
    }

    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        CompletableFuture<?>[] futures = stages.stream().map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> {
            List<T> results = new ArrayList<>(stages.size());
            stages.forEach(stage -> results.add(stage.toCompletableFuture().join()));
            return List.copyOf(results);
        });
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }

    private record Checks(boolean duplicate, long dailyCount) {
    }
}
