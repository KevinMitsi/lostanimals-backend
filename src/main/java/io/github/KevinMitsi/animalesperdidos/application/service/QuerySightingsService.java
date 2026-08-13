package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.QuerySightingsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.nio.charset.StandardCharsets;
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
        var criteria = new SightingRepository.SearchCriteria(reporterId, search.species(), search.neighborhoodId(),
                search.status(), search.cursorCreatedAt(), search.cursorId(), limit + 1);
        return repository.search(criteria).thenCompose(found -> {
            boolean next = found.size() > limit; List<Sighting> selected = found.stream().limit(limit).toList();
            return sequence(selected.stream().map(s -> view(s, exact)).toList()).thenApply(items ->
                    new Page(items, next && !selected.isEmpty() ? cursor(selected.getLast()) : null));
        });
    }
    private CompletionStage<View> view(Sighting sighting, boolean exact) {
        return sequence(sighting.images().stream().map(image -> storage.createDownloadUrl(image.objectKey(), Duration.ofMinutes(15))
                .thenApply(url -> new ImageView(image.id(), url, image.primary(), image.sortOrder()))).toList())
                .thenApply(images -> new View(sighting.id(), sighting.species(), sighting.description(), sighting.observedAt(),
                        exact ? sighting.location().latitude() : approximate(sighting.location().latitude()),
                        exact ? sighting.location().longitude() : approximate(sighting.location().longitude()),
                        sighting.neighborhoodId(), sighting.status(), images, sighting.createdAt(), sighting.updatedAt(), sighting.version()));
    }
    private static double approximate(double value) { return Math.round(value * 1000d) / 1000d; }
    private static String cursor(Sighting s) { return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((s.createdAt() + "|" + s.id()).getBytes(StandardCharsets.UTF_8)); }
    private static <T> CompletionStage<List<T>> sequence(List<? extends CompletionStage<T>> stages) {
        var futures = stages.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> stages.stream().map(s -> s.toCompletableFuture().join()).toList());
    }
}
