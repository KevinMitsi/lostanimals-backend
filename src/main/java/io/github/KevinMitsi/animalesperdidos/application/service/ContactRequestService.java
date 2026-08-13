package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ContactRequestUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class ContactRequestService implements ContactRequestUseCase {
    private final ContactRepository contacts;
    private final LostPetReportRepository reports;
    private final SightingRepository sightings;
    private final Clock clock;

    public ContactRequestService(ContactRepository contacts, LostPetReportRepository reports,
                                 SightingRepository sightings, Clock clock) {
        this.contacts = contacts; this.reports = reports; this.sightings = sightings; this.clock = clock;
    }

    @Override public CompletionStage<UUID> create(UUID actorId, Command command) {
        PublicationRef publication = new PublicationRef(command.publicationType(), command.publicationId());
        return owner(publication).thenCompose(owner -> {
            if (owner.equals(actorId)) return failed(new BusinessRuleViolation("Cannot contact your own publication"));
            return contacts.blockedBetween(actorId, owner).thenCompose(blocked -> {
                if (blocked) return failed(new ForbiddenOperation());
                ContactRequest request = ContactRequest.create(UUID.randomUUID(), publication, actorId, owner,
                        command.note(), clock.instant());
                return contacts.saveRequest(request).thenApply(ContactRequest::id);
            });
        });
    }

    @Override public CompletionStage<List<View>> received(UUID actorId) {
        return contacts.requestsFor(actorId, true).thenApply(values -> values.stream().map(this::view).toList());
    }
    @Override public CompletionStage<List<View>> sent(UUID actorId) {
        return contacts.requestsFor(actorId, false).thenApply(values -> values.stream().map(this::view).toList());
    }
    @Override public CompletionStage<UUID> accept(UUID actorId, UUID requestId) {
        return request(requestId).thenCompose(request -> {
            ContactRequest accepted;
            try { accepted = request.accept(actorId, clock.instant()); }
            catch (IllegalStateException error) { return failed(new ForbiddenOperation()); }
            Conversation conversation = Conversation.open(UUID.randomUUID(), request.id(), request.requesterId(),
                    request.recipientId(), clock.instant());
            return contacts.blockedBetween(request.requesterId(), request.recipientId()).thenCompose(blocked -> {
                if (blocked) return failed(new ForbiddenOperation());
                return contacts.accept(accepted, conversation).thenApply(Conversation::id);
            });
        });
    }
    @Override public CompletionStage<Void> reject(UUID actorId, UUID requestId) {
        return request(requestId).thenCompose(value -> {
            try { return contacts.updateRequest(value.reject(actorId, clock.instant())).thenApply(ignored -> null); }
            catch (IllegalStateException error) { return failed(new ForbiddenOperation()); }
        });
    }
    @Override public CompletionStage<Void> cancel(UUID actorId, UUID requestId) {
        return request(requestId).thenCompose(value -> {
            try { return contacts.updateRequest(value.cancel(actorId)).thenApply(ignored -> null); }
            catch (IllegalStateException error) { return failed(new ForbiddenOperation()); }
        });
    }
    private CompletionStage<UUID> owner(PublicationRef publication) {
        if (publication.type() == PublicationType.LOST_PET_REPORT) {
            return reports.findById(publication.id()).thenCompose(value -> value
                    .filter(report -> report.status() == ReportStatus.LOST).<CompletionStage<UUID>>map(report -> completed(report.ownerId()))
                    .orElseGet(() -> failed(new ResourceNotFound("Active publication"))));
        }
        return sightings.findById(publication.id()).thenCompose(value -> value
                .filter(sighting -> sighting.status() == SightingStatus.ACTIVE).<CompletionStage<UUID>>map(s -> completed(s.reporterId()))
                .orElseGet(() -> failed(new ResourceNotFound("Active publication"))));
    }
    private CompletionStage<ContactRequest> request(UUID id) {
        return contacts.findRequest(id).thenCompose(value -> value.<CompletionStage<ContactRequest>>map(ContactRequestService::completed)
                .orElseGet(() -> failed(new ResourceNotFound("Contact request"))));
    }
    private View view(ContactRequest value) { return new View(value.id(), value.publication().type(), value.publication().id(),
            value.requesterId(), value.recipientId(), value.status(), value.note(), value.createdAt(), value.answeredAt()); }
    private static <T> CompletionStage<T> completed(T value) { return CompletableFuture.completedFuture(value); }
    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
