package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.QueryLostPetReportsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetImage;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class QueryLostPetReportsService implements QueryLostPetReportsUseCase {
    private static final Duration IMAGE_URL_TTL = Duration.ofMinutes(15);
    private final LostPetReportRepository repository;
    private final ImageStoragePort storage;

    public QueryLostPetReportsService(LostPetReportRepository repository, ImageStoragePort storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Override
    public CompletionStage<ReportView> getPublic(UUID reportId) {
        return repository.findById(reportId)
                .thenCompose(optional -> optional.<CompletionStage<ReportView>>map(report -> toView(report, false))
                        .orElseGet(() -> CompletableFuture.failedFuture(new ResourceNotFound("Lost-pet report"))));
    }

    @Override
    public CompletionStage<Page> searchPublic(Search command) {
        return search(null, command, false);
    }

    @Override
    public CompletionStage<Page> mine(UUID ownerId, Search command) {
        return search(ownerId, command, true);
    }

    private CompletionStage<Page> search(UUID ownerId, Search command, boolean exactLocation) {
        int limit = Math.max(1, Math.min(command.limit(), 50));
        SearchCriteriaPolicy.validateRange(command.from(), command.to());
        var area = SearchCriteriaPolicy.area(command.latitude(), command.longitude(), command.radiusMeters());
        var cursor = SearchCriteriaPolicy.decode(command.cursor());
        LostPetReportRepository.SearchCriteria criteria = new LostPetReportRepository.SearchCriteria(ownerId,
                command.species(), command.departmentId(), command.cityId(), command.neighborhoodId(), command.status(),
                command.from(), command.to(), area, exactLocation, cursor.createdAt(), cursor.id(), limit + 1);
        return repository.search(criteria).thenCompose(found -> {
            boolean hasNext = found.size() > limit;
            List<LostPetReport> selected = found.stream().limit(limit).toList();
            List<CompletionStage<ReportView>> views = selected.stream()
                    .map(report -> toView(report, exactLocation)).toList();
            return sequence(views).thenApply(items -> {
                String nextCursor = hasNext && !selected.isEmpty()
                        ? SearchCriteriaPolicy.encode(selected.getLast().createdAt(), selected.getLast().id()) : null;
                return new Page(items, nextCursor);
            });
        });
    }

    private CompletionStage<ReportView> toView(LostPetReport report, boolean exactLocation) {
        List<CompletionStage<ImageView>> images = report.images().stream().map(this::toImageView).toList();
        return sequence(images).thenApply(imageViews -> new ReportView(report.id(), report.petName(), report.species(),
                report.description(), report.disappearedAt(),
                exactLocation ? report.lastSeenAt().latitude() : approximate(report.lastSeenAt().latitude()),
                exactLocation ? report.lastSeenAt().longitude() : approximate(report.lastSeenAt().longitude()),
                report.neighborhoodId(), report.status(), imageViews, report.createdAt(), report.updatedAt(), report.version()));
    }

    private CompletionStage<ImageView> toImageView(LostPetImage image) {
        return storage.createDownloadUrl(image.objectKey(), IMAGE_URL_TTL)
                .thenApply(url -> new ImageView(image.id(), url, image.primary(), image.sortOrder()));
    }

    private static double approximate(double coordinate) { return Math.round(coordinate * 1000d) / 1000d; }

    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        CompletableFuture<?>[] futures = stages.stream().map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> {
            List<T> result = new ArrayList<>(stages.size());
            stages.forEach(stage -> result.add(stage.toCompletableFuture().join()));
            return List.copyOf(result);
        });
    }
}
