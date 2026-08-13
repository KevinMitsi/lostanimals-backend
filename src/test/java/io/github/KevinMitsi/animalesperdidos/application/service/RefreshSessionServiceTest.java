package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidOrExpiredToken;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RefreshSessionUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RefreshSessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final Duration TTL = Duration.ofDays(30);

    @Mock RefreshSessionRepository sessions;
    @Mock UserRepository users;
    @Mock OpaqueTokenPort opaqueTokens;
    @Mock TokenIssuerPort accessTokens;
    private RefreshSessionService service;

    @BeforeEach
    void setUp() {
        service = new RefreshSessionService(sessions, users, opaqueTokens, accessTokens,
                Clock.fixed(NOW, ZoneOffset.UTC), TTL);
        lenient().when(opaqueTokens.generate()).thenReturn(new OpaqueTokenPort.TokenPair("raw-new", "hash-new"));
        lenient().when(opaqueTokens.hash(anyString())).thenAnswer(invocation -> "hash-of:" + invocation.getArgument(0));
    }

    @Test
    void rotatesRefreshTokenAndIssuesNewAccessToken() {
        User user = AuthenticationServicesTest.registeredUser().verifyEmail(NOW);
        RefreshSession replacement = new RefreshSession(UUID.randomUUID(), user.id(), "hash-new",
                NOW.plus(TTL), null, null, NOW);
        when(sessions.rotate(eq("hash-of:old"), any(UUID.class), eq("hash-new"), eq(NOW.plus(TTL)), eq(NOW)))
                .thenReturn(completed(Optional.of(replacement)));
        when(users.findById(user.id())).thenReturn(completed(Optional.of(user)));
        when(accessTokens.issue(user)).thenReturn(new TokenIssuerPort.IssuedToken("jwt", 3600));

        RefreshSessionUseCase.Result result = service.refresh("old").toCompletableFuture().join();

        assertEquals("jwt", result.accessToken());
        assertEquals("raw-new", result.refreshToken());
        verify(sessions).rotate(eq("hash-of:old"), any(UUID.class), eq("hash-new"), eq(NOW.plus(TTL)), eq(NOW));
    }

    @Test
    void rejectsARefreshTokenThatCannotBeRotated() {
        when(sessions.rotate(eq("hash-of:reused"), any(UUID.class), eq("hash-new"), any(), eq(NOW)))
                .thenReturn(completed(Optional.empty()));

        CompletionException error = assertThrows(CompletionException.class,
                () -> service.refresh("reused").toCompletableFuture().join());

        assertInstanceOf(InvalidOrExpiredToken.class, error.getCause());
        verifyNoInteractions(users, accessTokens);
    }
}
