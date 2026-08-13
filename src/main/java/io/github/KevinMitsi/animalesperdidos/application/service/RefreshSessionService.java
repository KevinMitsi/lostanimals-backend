package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidOrExpiredToken;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RefreshSessionUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.OpaqueTokenPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.RefreshSessionRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class RefreshSessionService implements RefreshSessionUseCase {
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final OpaqueTokenPort opaqueTokens;
    private final TokenIssuerPort accessTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public RefreshSessionService(RefreshSessionRepository sessions, UserRepository users,
                                 OpaqueTokenPort opaqueTokens, TokenIssuerPort accessTokens,
                                 Clock clock, Duration refreshTtl) {
        this.sessions = sessions;
        this.users = users;
        this.opaqueTokens = opaqueTokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public CompletionStage<Result> refresh(String rawToken) {
        Instant now = clock.instant();
        OpaqueTokenPort.TokenPair replacementPair = opaqueTokens.generate();
        UUID replacementId = UUID.randomUUID();
        return sessions.rotate(opaqueTokens.hash(rawToken), replacementId, replacementPair.hash(),
                        now.plus(refreshTtl), now)
                .thenCompose(optional -> {
                    if (optional.isEmpty()) return failed(new InvalidOrExpiredToken());
                    return users.findById(optional.get().userId()).thenCompose(user -> {
                        if (user.isEmpty()) return failed(new InvalidOrExpiredToken());
                        TokenIssuerPort.IssuedToken access = accessTokens.issue(user.get());
                        return CompletableFuture.completedFuture(new Result(access.value(), replacementPair.rawValue(),
                                "Bearer", access.expiresInSeconds(), refreshTtl.toSeconds()));
                    });
                });
    }

    @Override
    public CompletionStage<Void> logout(String rawToken) {
        return sessions.revoke(opaqueTokens.hash(rawToken), clock.instant()).thenApply(ignored -> null);
    }

    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
