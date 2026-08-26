package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.QuerySightingsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class QuerySightingsService implements QuerySightingsUseCase {
    private final SightingRepository repository; private final ImageStoragePort storage;
    public QuerySightingsService(SightingRepository repository, ImageStoragePort storage) {
        this.repository = repository; this.storage = storage;
    }
    @Override public CompletionStage<View> getPublic(UUID id) {
        return repository.findById(id).thenCompose(optional -> optional.<CompletionStage<View>>map(s -> view(s, false))
                .orElseGet(() -> CompletableFuture.failedFuture(new ResourceNotFound("Sighting"))));
    }
    @Override public CompletionStage<Page> searchPublic(Search search) { return search(null, search, false); }
    @Override public CompletionStage<Page> mine(UUID reporterId, Search search) { return search(reporterId, search, true); }
    private CompletionStage<Page> search(UUID reporterId, Search search, boolean exact) {
        int limit = Math.max(1, Math.min(search.limit(), 50));
        SearchCriteriaPolicy.validateRange(search.from(), search.to());
        String neighborhood = SearchCriteriaPolicy.validateLocationFilters(search.departmentCode(),
                search.municipalityCode(), search.neighborhood());
        var area = SearchCriteriaPolicy.area(search.latitude(), search.longitude(), search.radiusMeters());
        var cursor = SearchCriteriaPolicy.decode(search.cursor());
        var criteria = new SightingRepository.SearchCriteria(reporterId, search.species(), search.departmentCode(),
                search.municipalityCode(), neighborhood, search.status(), search.from(), search.to(), area,
                exact, cursor.createdAt(), cursor.id(), limit + 1);
        return repository.search(criteria).thenCompose(found -> {
            boolean next = found.size() > limit; List<Sighting> selected = found.stream().limit(limit).toList();
            return sequence(selected.stream().map(s -> view(s, exact)).toList()).thenApply(items ->
                    new Page(items, next && !selected.isEmpty()
                            ? SearchCriteriaPolicy.encode(selected.getLast().createdAt(), selected.getLast().id()) : null));
        });
    }
    private CompletionStage<View> view(Sighting sighting, boolean exact) {
        return sequence(sighting.images().stream().map(image -> storage.createDownloadUrl(image.objectKey(), Duration.ofMinutes(15))
                .thenApply(url -> new ImageView(image.id(), url, image.primary(), image.sortOrder()))).toList())
                .thenApply(images -> new View(sighting.id(), sighting.species(), sighting.description(), sighting.observedAt(),
                        exact ? sighting.location().latitude() : approximate(sighting.location().latitude()),
                        exact ? sighting.location().longitude() : approximate(sighting.location().longitude()),
                        sighting.administrativeLocation().departmentCode(),
                        sighting.administrativeLocation().municipalityCode(),
                        sighting.administrativeLocation().neighborhood(), sighting.status(), images,
                        sighting.createdAt(), sighting.updatedAt(), sighting.version()));
    }
    private static double approximate(double value) { return Math.round(value * 1000d) / 1000d; }
    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        var futures = stages.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> stages.stream().map(s -> s.toCompletableFuture().join()).toList());
    }
}
