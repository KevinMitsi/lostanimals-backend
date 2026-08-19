package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.port.in.CompleteGoogleProfileUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class CompleteGoogleProfileService implements CompleteGoogleProfileUseCase {
    private final UserRepository users;

    public CompleteGoogleProfileService(UserRepository users) {
        this.users = users;
    }

    @Override
    public CompletionStage<Result> complete(Command command) {
        return users.findById(command.userId())
                .thenCompose(found -> found.<CompletionStage<User>>map(user -> checkUniqueness(command)
                                .thenCompose(ignored -> users.update(user.completeProfile(
                                        command.phone(), command.documentNumber()))))
                        .orElseGet(() -> CompletableFuture.failedFuture(new ResourceNotFound("User not found"))))
                .thenApply(this::toResult);
    }

    private CompletionStage<Void> checkUniqueness(Command command) {
        return users.existsByPhone(command.phone())
                .thenCombine(users.existsByDocumentNumber(command.documentNumber()), Checks::new)
                .thenCompose(checks -> {
                    if (checks.phone()) return CompletableFuture.failedFuture(new DuplicateUserData("phone"));
                    if (checks.document()) return CompletableFuture.failedFuture(new DuplicateUserData("document number"));
                    return CompletableFuture.completedFuture(null);
                });
    }

    private Result toResult(User user) {
        return new Result(user.id(), user.email(), user.displayName(), user.phone(), user.documentNumber(),
                user.pictureUrl(), user.isProfileComplete());
    }

    private record Checks(boolean phone, boolean document) {
    }
}
