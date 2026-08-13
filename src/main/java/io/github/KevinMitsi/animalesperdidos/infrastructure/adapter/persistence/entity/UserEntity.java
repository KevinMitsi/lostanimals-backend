package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record UserEntity(UUID id, String email, String passwordHash, String phone, String documentNumber,
                         String displayName, Instant habeasDataAcceptedAt, Instant createdAt) {
}
