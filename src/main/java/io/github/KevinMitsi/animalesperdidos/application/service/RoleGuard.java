package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import java.util.*;
import java.util.concurrent.*;

final class RoleGuard {
    private RoleGuard() { }
    static CompletionStage<Void> require(UserRepository users, UUID actorId, UserRole... roles) {
        Set<UserRole> allowed=Set.of(roles);
        return users.findById(actorId).thenCompose(user -> user.filter(value->allowed.contains(value.role())).isPresent()
                ? CompletableFuture.completedFuture(null) : CompletableFuture.failedFuture(new ForbiddenOperation()));
    }
}
