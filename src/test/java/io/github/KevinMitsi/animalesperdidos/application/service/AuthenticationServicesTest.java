package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.exception.EmailNotVerified;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServicesTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    @Mock UserRepository users;
    @Mock PasswordHasherPort passwords;
    @Mock BotVerificationPort botVerification;
    @Mock AccountTokenRepository accountTokens;
    @Mock OpaqueTokenPort opaqueTokens;
    @Mock AccountNotificationPort notifications;
    @Mock RefreshSessionRepository sessions;
    @Mock TokenIssuerPort accessTokens;
    @Captor ArgumentCaptor<User> userCaptor;
    @Captor ArgumentCaptor<AccountToken> accountTokenCaptor;
    @Captor ArgumentCaptor<RefreshSession> sessionCaptor;

    @BeforeEach
    void commonBehavior() {
        lenient().when(botVerification.verify(anyString(), nullable(String.class), anyString()))
                .thenReturn(completed(true));
        lenient().when(opaqueTokens.generate())
                .thenReturn(new OpaqueTokenPort.TokenPair("raw-token", "hashed-token"));
    }

    @Test
    void registersAndCreatesEmailVerification() {
        when(users.existsByEmail(anyString())).thenReturn(completed(false));
        when(users.existsByPhone(anyString())).thenReturn(completed(false));
        when(users.existsByDocumentNumber(anyString())).thenReturn(completed(false));
        when(passwords.hash("ClaveMuySegura2026")).thenReturn(completed("hash:ClaveMuySegura2026"));
        when(users.save(any(User.class))).thenAnswer(invocation -> completed(invocation.getArgument(0)));
        when(accountTokens.replaceActive(any())).thenReturn(completed(null));
        when(notifications.sendEmailVerification(anyString(), anyString(), anyString())).thenReturn(completed(null));

        RegisterUserUseCase.Result result = registration().register(new RegisterUserUseCase.Command(
                " ANA@Example.com ", "ClaveMuySegura2026", "+573001234567", "1094912345",
                "Ana García", true, "turnstile", "127.0.0.1")).toCompletableFuture().join();

        verify(users).save(userCaptor.capture());
        verify(accountTokens).replaceActive(accountTokenCaptor.capture());
        verify(notifications).sendEmailVerification("ana@example.com", "Ana García", "raw-token");
        assertEquals("ana@example.com", result.email());
        assertEquals("hash:ClaveMuySegura2026", userCaptor.getValue().passwordHash());
        assertFalse(userCaptor.getValue().isEmailVerified());
        assertEquals(AccountToken.Type.EMAIL_VERIFICATION, accountTokenCaptor.getValue().type());
    }

    @Test
    void rejectsDuplicatePhoneBeforeHashing() {
        when(users.existsByEmail(anyString())).thenReturn(completed(false));
        when(users.existsByPhone(anyString())).thenReturn(completed(true));
        when(users.existsByDocumentNumber(anyString())).thenReturn(completed(false));

        CompletionException error = assertThrows(CompletionException.class, () -> registration()
                .register(command()).toCompletableFuture().join());

        assertInstanceOf(DuplicateUserData.class, error.getCause());
        verifyNoInteractions(passwords, accountTokens, notifications);
        verify(users, never()).save(any());
    }

    @Test
    void verifiedUserAuthenticatesAndReceivesRotatableSession() {
        User user = registeredUser().verifyEmail(NOW);
        when(users.findByEmail("ana@example.com")).thenReturn(completed(Optional.of(user)));
        when(passwords.matches("secret", "hash:secret")).thenReturn(completed(true));
        when(accessTokens.issue(user)).thenReturn(new TokenIssuerPort.IssuedToken("signed.jwt", 3600));
        when(sessions.save(any())).thenReturn(completed(null));

        AuthenticateUserUseCase.Result result = authentication().authenticate(
                new AuthenticateUserUseCase.Command("ANA@example.com", "secret", "token", null))
                .toCompletableFuture().join();

        verify(sessions).save(sessionCaptor.capture());
        assertEquals("signed.jwt", result.accessToken());
        assertEquals("raw-token", result.refreshToken());
        assertEquals(user.id(), sessionCaptor.getValue().userId());
    }

    @Test
    void rejectsLoginUntilEmailIsVerified() {
        User user = registeredUser();
        when(users.findByEmail(anyString())).thenReturn(completed(Optional.of(user)));
        when(passwords.matches("secret", "hash:secret")).thenReturn(completed(true));

        CompletionException error = assertThrows(CompletionException.class, () -> authentication()
                .authenticate(new AuthenticateUserUseCase.Command("ana@example.com", "secret", "token", null))
                .toCompletableFuture().join());

        assertInstanceOf(EmailNotVerified.class, error.getCause());
        verifyNoInteractions(accessTokens, sessions);
    }

    @Test
    void doesNotRevealWhetherLoginEmailExists() {
        when(users.findByEmail(anyString())).thenReturn(completed(Optional.empty()));

        CompletionException error = assertThrows(CompletionException.class, () -> authentication()
                .authenticate(new AuthenticateUserUseCase.Command("nobody@example.com", "secret", "token", null))
                .toCompletableFuture().join());

        assertInstanceOf(InvalidCredentials.class, error.getCause());
        verifyNoInteractions(passwords, accessTokens, sessions);
    }

    private RegisterUserService registration() {
        return new RegisterUserService(users, passwords, botVerification, CLOCK, accountTokens, opaqueTokens,
                notifications, Duration.ofHours(24));
    }

    private AuthenticateUserService authentication() {
        return new AuthenticateUserService(users, passwords, accessTokens, botVerification, sessions,
                opaqueTokens, CLOCK, REFRESH_TTL);
    }

    private static RegisterUserUseCase.Command command() {
        return new RegisterUserUseCase.Command("ana@example.com", "ClaveMuySegura2026", "+573001234567",
                "1094912345", "Ana García", true, "turnstile", null);
    }

    static User registeredUser() {
        return User.register(UUID.randomUUID(), "ana@example.com", "hash:secret", "+573001234567",
                "1094912345", "Ana", NOW);
    }

    static <T> CompletableFuture<T> completed(T value) { return CompletableFuture.completedFuture(value); }
}
