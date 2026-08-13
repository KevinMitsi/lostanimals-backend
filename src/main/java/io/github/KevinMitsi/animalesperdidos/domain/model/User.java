package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String email, String passwordHash, String phone, String documentNumber,
                   String displayName, Instant habeasDataAcceptedAt, Instant emailVerifiedAt, Instant createdAt) {

    public User {
        Objects.requireNonNull(id);
        Objects.requireNonNull(habeasDataAcceptedAt);
        Objects.requireNonNull(createdAt);
        email = requireText(email, "email").toLowerCase(Locale.ROOT);
        passwordHash = requireText(passwordHash, "passwordHash");
        phone = requireText(phone, "phone");
        documentNumber = requireText(documentNumber, "documentNumber");
        displayName = requireText(displayName, "displayName");
        if (habeasDataAcceptedAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Data processing acceptance cannot be in the future");
        }
        if (emailVerifiedAt != null && emailVerifiedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Email verification cannot predate account creation");
        }
    }

    public static User register(UUID id, String email, String passwordHash, String phone,
                                String documentNumber, String displayName, Instant now) {
        return new User(id, email, passwordHash, phone, documentNumber, displayName, now, null, now);
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public User verifyEmail(Instant verifiedAt) {
        Objects.requireNonNull(verifiedAt);
        return new User(id, email, passwordHash, phone, documentNumber, displayName,
                habeasDataAcceptedAt, verifiedAt, createdAt);
    }

    public User changePassword(String newPasswordHash) {
        return new User(id, email, newPasswordHash, phone, documentNumber, displayName,
                habeasDataAcceptedAt, emailVerifiedAt, createdAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
