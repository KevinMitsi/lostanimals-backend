package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.entity;

import java.time.Instant;
import java.util.UUID;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;

public record UserEntity(UUID id, String email, String passwordHash, String phone, String documentNumber,
                         String displayName, UserRole role, Instant habeasDataAcceptedAt, Instant emailVerifiedAt, Instant createdAt) {
}
