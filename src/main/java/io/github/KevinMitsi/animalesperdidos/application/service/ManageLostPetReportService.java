package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ManageLostPetReportUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ServiceAreaRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public final class ManageLostPetReportService implements ManageLostPetReportUseCase {
    private final LostPetReportRepository repository;
    private final ImageStoragePort storage;
    private final Clock clock;
    private final ServiceAreaRepository serviceAreas;

    public ManageLostPetReportService(LostPetReportRepository repository, ImageStoragePort storage, Clock clock,
                                      ServiceAreaRepository serviceAreas) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
        this.serviceAreas = serviceAreas;
    }

    @Override
    public CompletionStage<Void> edit(UUID actorId, UUID reportId, Edit command) {
        return requireEnabled(command.neighborhoodId()).thenCompose(ignored -> mutate(actorId, reportId,
                report -> report.edit(command.petName(), command.species(), command.description(),
                command.disappearedAt(), new GeoPoint(command.latitude(), command.longitude()),
                command.neighborhoodId(), clock.instant()))).thenApply(ignored -> null);
    }

    @Override
    public CompletionStage<Void> close(UUID actorId, UUID reportId, ReportStatus status) {
        if (status == ReportStatus.REUNITED) throw new ForbiddenOperation();
        return mutate(actorId, reportId, report -> status == ReportStatus.LOST
                ? report.reopen(clock.instant(), Duration.ofDays(30))
                : report.changeStatus(status, clock.instant())).thenApply(ignored -> null);
    }

    @Override
    public CompletionStage<UUID> addImage(UUID actorId, UUID reportId, String objectKey) {
        String requiredPrefix = "lost-pet-reports/staging/users/" + actorId + "/";
        if (!objectKey.startsWith(requiredPrefix)) throw new BusinessRuleViolation("Image does not belong to the user");
        return storage.sanitize(actorId, ImageStoragePort.Category.LOST_PET_REPORT, objectKey)
                .exceptionallyCompose(error -> failed(new BusinessRuleViolation("Uploaded image could not be validated")))
                .thenCompose(object -> {
            if (!validImage(object)) return failed(new BusinessRuleViolation("Uploaded image is invalid"));
            UUID imageId = UUID.randomUUID();
            return mutate(actorId, reportId, report -> report.addImage(
                    new LostPetImage(imageId, object.objectKey(), false, report.images().size()), clock.instant()))
                    .thenApply(ignored -> imageId)
                    .exceptionallyCompose(error -> storage.delete(object.objectKey())
                            .thenCompose(ignored -> failed(error)));
        });
    }

    @Override
    public CompletionStage<Void> removeImage(UUID actorId, UUID reportId, UUID imageId) {
        return owned(actorId, reportId).thenCompose(report -> {
            String key = report.image(imageId).objectKey();
            return repository.update(report.removeImage(imageId, clock.instant()))
                    .thenCompose(updated -> storage.delete(key).exceptionally(ignored -> null))
                    .thenApply(ignored -> null);
        });
    }

    @Override
    public CompletionStage<Void> setPrimaryImage(UUID actorId, UUID reportId, UUID imageId) {
        return mutate(actorId, reportId, report -> report.setPrimaryImage(imageId, clock.instant())).thenApply(ignored -> null);
    }

    private CompletionStage<LostPetReport> mutate(UUID actorId, UUID reportId,
                                                   Function<LostPetReport, LostPetReport> mutation) {
        return owned(actorId, reportId).thenCompose(report -> repository.update(mutation.apply(report)));
    }

    private CompletionStage<LostPetReport> owned(UUID actorId, UUID reportId) {
        return repository.findById(reportId).thenCompose(optional -> {
            if (optional.isEmpty()) return failed(new ResourceNotFound("Lost-pet report"));
            if (!optional.get().ownerId().equals(actorId)) return failed(new ForbiddenOperation());
            return CompletableFuture.completedFuture(optional.get());
        });
    }

    private CompletionStage<Void> requireEnabled(UUID neighborhoodId) {
        return serviceAreas.isNeighborhoodEnabled(neighborhoodId).thenCompose(enabled -> enabled
                ? CompletableFuture.completedFuture(null)
                : failed(new BusinessRuleViolation("Publication area is not enabled")));
    }

    private static boolean validImage(ImageStoragePort.StoredObject object) {
        return object.contentLength() > 0 && object.contentLength() <= 8L * 1024 * 1024
                && List.of("image/jpeg", "image/png").contains(object.contentType())
                && object.checksumSha256() != null;
    }

    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
