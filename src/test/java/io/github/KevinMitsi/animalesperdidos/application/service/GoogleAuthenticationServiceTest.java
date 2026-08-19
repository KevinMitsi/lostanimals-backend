package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.in.CompleteGoogleProfileUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.GoogleAuthenticationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.GoogleIdentityPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.OpaqueTokenPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.RefreshSessionRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-19T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final GoogleIdentityPort.Identity IDENTITY = new GoogleIdentityPort.Identity(
            "google-subject", "ana@example.com", true, "Ana García", "https://example.com/photo.jpg");

    @Mock UserRepository users;
    @Mock GoogleIdentityPort google;
    @Mock TokenIssuerPort accessTokens;
    @Mock RefreshSessionRepository sessions;
    @Mock OpaqueTokenPort opaqueTokens;
    @Captor ArgumentCaptor<User> userCaptor;
    @Captor ArgumentCaptor<RefreshSession> sessionCaptor;

    @BeforeEach
    void sessionBehavior() {
        lenient().when(google.verify("google-id-token")).thenReturn(done(IDENTITY));
        lenient().when(accessTokens.issue(any())).thenReturn(new TokenIssuerPort.IssuedToken("signed.jwt", 3600));
        lenient().when(opaqueTokens.generate()).thenReturn(new OpaqueTokenPort.TokenPair("refresh", "refresh-hash"));
        lenient().when(sessions.save(any())).thenReturn(done(null));
    }

    @Test
    void createsGoogleUserWithVerifiedEmailAndIncompleteProfile() {
        when(users.findByGoogleSubject("google-subject")).thenReturn(done(Optional.empty()));
        when(users.findByEmail("ana@example.com")).thenReturn(done(Optional.empty()));
        when(users.save(any())).thenAnswer(invocation -> done(invocation.getArgument(0)));

        GoogleAuthenticationUseCase.Result result = service().authenticate(
                new GoogleAuthenticationUseCase.Command("google-id-token", true)).toCompletableFuture().join();

        verify(users).save(userCaptor.capture());
        verify(sessions).save(sessionCaptor.capture());
        User saved = userCaptor.getValue();
        assertNull(saved.passwordHash());
        assertNull(saved.phone());
        assertEquals("google-subject", saved.googleSubject());
        assertTrue(saved.isEmailVerified());
        assertFalse(result.profileComplete());
        assertTrue(result.newUser());
        assertEquals(saved.id(), sessionCaptor.getValue().userId());
    }

    @Test
    void requiresConsentOnlyWhenGoogleWouldCreateAnAccount() {
        when(users.findByGoogleSubject("google-subject")).thenReturn(done(Optional.empty()));
        when(users.findByEmail("ana@example.com")).thenReturn(done(Optional.empty()));

        CompletionException error = assertThrows(CompletionException.class, () -> service().authenticate(
                new GoogleAuthenticationUseCase.Command("google-id-token", false)).toCompletableFuture().join());

        assertInstanceOf(BusinessRuleViolation.class, error.getCause());
        verify(users, never()).save(any());
        verifyNoInteractions(sessions);
    }

    @Test
    void linksVerifiedGoogleIdentityToExistingEmailAccount() {
        User local = User.register(UUID.randomUUID(), "ana@example.com", "hash", "+573001234567",
                "1094912345", "Ana", NOW);
        when(users.findByGoogleSubject("google-subject")).thenReturn(done(Optional.empty()));
        when(users.findByEmail("ana@example.com")).thenReturn(done(Optional.of(local)));
        when(users.update(any())).thenAnswer(invocation -> done(invocation.getArgument(0)));

        GoogleAuthenticationUseCase.Result result = service().authenticate(
                new GoogleAuthenticationUseCase.Command("google-id-token", false)).toCompletableFuture().join();

        verify(users).update(userCaptor.capture());
        assertEquals("google-subject", userCaptor.getValue().googleSubject());
        assertTrue(userCaptor.getValue().isEmailVerified());
        assertTrue(result.profileComplete());
        assertFalse(result.newUser());
    }

    @Test
    void rejectsGoogleIdentityWithoutVerifiedEmail() {
        GoogleIdentityPort.Identity unverified = new GoogleIdentityPort.Identity(
                "subject", "ana@example.com", false, "Ana", null);
        when(google.verify("google-id-token")).thenReturn(done(unverified));

        CompletionException error = assertThrows(CompletionException.class, () -> service().authenticate(
                new GoogleAuthenticationUseCase.Command("google-id-token", true)).toCompletableFuture().join());

        assertInstanceOf(InvalidCredentials.class, error.getCause());
        verifyNoInteractions(users, sessions);
    }

    @Test
    void completesMissingProfileFields() {
        User googleUser = User.registerWithGoogle(UUID.randomUUID(), "ana@example.com", "Ana",
                "google-subject", null, NOW);
        when(users.findById(googleUser.id())).thenReturn(done(Optional.of(googleUser)));
        when(users.existsByPhone("+573001234567")).thenReturn(done(false));
        when(users.existsByDocumentNumber("1094912345")).thenReturn(done(false));
        when(users.update(any())).thenAnswer(invocation -> done(invocation.getArgument(0)));

        CompleteGoogleProfileUseCase.Result result = new CompleteGoogleProfileService(users).complete(
                new CompleteGoogleProfileUseCase.Command(googleUser.id(), "+573001234567", "1094912345"))
                .toCompletableFuture().join();

        assertTrue(result.profileComplete());
        assertEquals("+573001234567", result.phone());
        assertEquals("1094912345", result.documentNumber());
    }

    @Test
    void rejectsDuplicateProfileData() {
        User googleUser = User.registerWithGoogle(UUID.randomUUID(), "ana@example.com", "Ana",
                "google-subject", null, NOW);
        when(users.findById(googleUser.id())).thenReturn(done(Optional.of(googleUser)));
        when(users.existsByPhone(any())).thenReturn(done(true));
        when(users.existsByDocumentNumber(any())).thenReturn(done(false));

        CompletionException error = assertThrows(CompletionException.class, () ->
                new CompleteGoogleProfileService(users).complete(new CompleteGoogleProfileUseCase.Command(
                        googleUser.id(), "+573001234567", "1094912345")).toCompletableFuture().join());

        assertInstanceOf(DuplicateUserData.class, error.getCause());
        verify(users, never()).update(any());
    }

    private GoogleAuthenticationService service() {
        return new GoogleAuthenticationService(users, google, accessTokens, sessions, opaqueTokens, CLOCK, REFRESH_TTL);
    }

    private static <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }
}
