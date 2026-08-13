package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidOrExpiredToken;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;
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
import java.util.concurrent.CompletionException;

import static io.github.KevinMitsi.animalesperdidos.application.service.AuthenticationServicesTest.completed;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock UserRepository users;
    @Mock AccountTokenRepository tokens;
    @Mock RefreshSessionRepository sessions;
    @Mock OpaqueTokenPort opaqueTokens;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AccountNotificationPort notifications;
    @Mock BotVerificationPort botVerification;
    @Captor ArgumentCaptor<User> userCaptor;
    private AccountLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new AccountLifecycleService(users, tokens, sessions, opaqueTokens, passwordHasher,
                notifications, CLOCK, Duration.ofHours(24), Duration.ofMinutes(30), botVerification);
        lenient().when(opaqueTokens.hash(anyString())).thenAnswer(invocation -> "hash-of:" + invocation.getArgument(0));
    }

    @Test
    void consumesVerificationTokenAndVerifiesUser() {
        User user = user();
        when(tokens.consume("hash-of:raw", AccountToken.Type.EMAIL_VERIFICATION, NOW))
                .thenReturn(completed(Optional.of(token(user.id(), AccountToken.Type.EMAIL_VERIFICATION))));
        when(users.findById(user.id())).thenReturn(completed(Optional.of(user)));
        when(users.update(any())).thenAnswer(invocation -> completed(invocation.getArgument(0)));

        service.verify("raw").toCompletableFuture().join();

        verify(users).update(userCaptor.capture());
        assertEquals(NOW, userCaptor.getValue().emailVerifiedAt());
    }

    @Test
    void resetChangesHashAndRevokesEverySession() {
        User user = user().verifyEmail(NOW.minusSeconds(60));
        when(tokens.consume("hash-of:raw", AccountToken.Type.PASSWORD_RESET, NOW))
                .thenReturn(completed(Optional.of(token(user.id(), AccountToken.Type.PASSWORD_RESET))));
        when(passwordHasher.hash("NuevaClaveSegura2026")).thenReturn(completed("hash:new"));
        when(users.findById(user.id())).thenReturn(completed(Optional.of(user)));
        when(users.update(any())).thenAnswer(invocation -> completed(invocation.getArgument(0)));
        when(sessions.revokeAllByUser(user.id(), NOW)).thenReturn(completed(null));

        service.reset("raw", "NuevaClaveSegura2026").toCompletableFuture().join();

        verify(users).update(userCaptor.capture());
        verify(sessions).revokeAllByUser(user.id(), NOW);
        assertEquals("hash:new", userCaptor.getValue().passwordHash());
    }

    @Test
    void usedOrExpiredTokenIsRejected() {
        when(tokens.consume(anyString(), eq(AccountToken.Type.EMAIL_VERIFICATION), eq(NOW)))
                .thenReturn(completed(Optional.empty()));

        CompletionException error = assertThrows(CompletionException.class,
                () -> service.verify("invalid").toCompletableFuture().join());

        assertInstanceOf(InvalidOrExpiredToken.class, error.getCause());
        verifyNoInteractions(users);
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(botVerification.verify("turnstile", "127.0.0.1", "password-recovery"))
                .thenReturn(completed(true));
        when(users.findByEmail("unknown@example.com")).thenReturn(completed(Optional.empty()));

        service.request("unknown@example.com", "turnstile", "127.0.0.1").toCompletableFuture().join();

        verifyNoInteractions(tokens, notifications);
    }

    private static User user() {
        return User.register(UUID.randomUUID(), "ana@example.com", "hash:old", "+573001234567",
                "1094912345", "Ana", NOW.minusSeconds(3600));
    }

    private static AccountToken token(UUID userId, AccountToken.Type type) {
        return new AccountToken(UUID.randomUUID(), userId, type, "hash-of:raw",
                NOW.plusSeconds(60), NOW, NOW.minusSeconds(60));
    }
}
