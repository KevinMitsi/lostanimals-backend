package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AdminUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class AdminService implements AdminUseCase {
    private final ServiceAreaRepository areas; private final UserRepository users; private final Clock clock;
    public AdminService(ServiceAreaRepository areas, UserRepository users, Clock clock) {
        this.areas = areas; this.users = users; this.clock = clock;
    }
    @Override public CompletionStage<List<ServiceAreaView>> serviceAreas(UUID adminId) {
        return RoleGuard.require(users,adminId,UserRole.ADMIN).thenCompose(ignored->areas.list())
                .thenApply(values -> values.stream().map(value -> new ServiceAreaView(value.cityId(),
                value.cityName(), value.departmentId(), value.departmentName(), value.enabled())).toList());
    }
    @Override public CompletionStage<Void> setServiceArea(UUID adminId, UUID cityId, boolean enabled) {
        return RoleGuard.require(users,adminId,UserRole.ADMIN)
                .thenCompose(ignored->areas.setEnabled(cityId, enabled, adminId, clock.instant()));
    }
    @Override public CompletionStage<Void> changeRole(UUID adminId, UUID userId, UserRole role) {
        return RoleGuard.require(users,adminId,UserRole.ADMIN).thenCompose(ignored->{
            if (adminId.equals(userId) && role != UserRole.ADMIN)
                return CompletableFuture.failedFuture(new BusinessRuleViolation("An administrator cannot remove their own role"));
            return users.findById(userId);
        })
                .thenCompose(optional -> optional.<CompletionStage<Void>>map(user ->
                users.update(user.changeRole(role)).thenApply(ignored -> null))
                .orElseGet(() -> CompletableFuture.failedFuture(new ResourceNotFound("User"))));
    }
}
