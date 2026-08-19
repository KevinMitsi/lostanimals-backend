package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String email, String passwordHash, String phone, String documentNumber,
                   String displayName, UserRole role, Instant habeasDataAcceptedAt, Instant emailVerifiedAt,
                   String googleSubject, String pictureUrl, Instant createdAt) {

    public User {
        Objects.requireNonNull(id);
        Objects.requireNonNull(habeasDataAcceptedAt);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(role);
        email = requireText(email, "email").toLowerCase(Locale.ROOT);
        passwordHash = optionalText(passwordHash);
        phone = optionalText(phone);
        documentNumber = optionalText(documentNumber);
        displayName = requireText(displayName, "displayName");
        googleSubject = optionalText(googleSubject);
        pictureUrl = optionalText(pictureUrl);
        if (passwordHash == null && googleSubject == null) {
            throw new IllegalArgumentException("At least one authentication method is required");
        }
        if (habeasDataAcceptedAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Data processing acceptance cannot be in the future");
        }
        if (emailVerifiedAt != null && emailVerifiedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Email verification cannot predate account creation");
        }
    }

    public static User register(UUID id, String email, String passwordHash, String phone,
                                String documentNumber, String displayName, Instant now) {
        return new User(id, email, passwordHash, phone, documentNumber, displayName, UserRole.USER,
                now, null, null, null, now);
    }

    public static User registerWithGoogle(UUID id, String email, String displayName, String googleSubject,
                                          String pictureUrl, Instant now) {
        return new User(id, email, null, null, null, displayName, UserRole.USER,
                now, now, googleSubject, pictureUrl, now);
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public User verifyEmail(Instant verifiedAt) {
        Objects.requireNonNull(verifiedAt);
        return new User(id, email, passwordHash, phone, documentNumber, displayName, role,
                habeasDataAcceptedAt, verifiedAt, googleSubject, pictureUrl, createdAt);
    }

    public User changePassword(String newPasswordHash) {
        return new User(id, email, newPasswordHash, phone, documentNumber, displayName, role,
                habeasDataAcceptedAt, emailVerifiedAt, googleSubject, pictureUrl, createdAt);
    }

    public User changeRole(UserRole newRole) {
        return new User(id, email, passwordHash, phone, documentNumber, displayName,
                Objects.requireNonNull(newRole), habeasDataAcceptedAt, emailVerifiedAt,
                googleSubject, pictureUrl, createdAt);
    }

    public User linkGoogle(String subject, String googlePictureUrl) {
        if (googleSubject != null && !googleSubject.equals(subject)) {
            throw new IllegalStateException("A different Google account is already linked");
        }
        return new User(id, email, passwordHash, phone, documentNumber, displayName, role,
                habeasDataAcceptedAt, emailVerifiedAt, subject, googlePictureUrl, createdAt);
    }

    public boolean isProfileComplete() {
        return phone != null && documentNumber != null;
    }

    public User completeProfile(String newPhone, String newDocumentNumber) {
        if (isProfileComplete()) {
            throw new IllegalStateException("The profile is already complete");
        }
        return new User(id, email, passwordHash, requireText(newPhone, "phone"),
                requireText(newDocumentNumber, "documentNumber"), displayName, role,
                habeasDataAcceptedAt, emailVerifiedAt, googleSubject, pictureUrl, createdAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
