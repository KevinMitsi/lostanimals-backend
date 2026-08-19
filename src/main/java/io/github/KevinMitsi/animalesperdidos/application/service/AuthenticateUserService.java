package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BotVerificationFailed;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.exception.EmailNotVerified;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.RefreshSessionRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.OpaqueTokenPort;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.util.Locale;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserRepository repository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final BotVerificationPort botVerification;
    private final RefreshSessionRepository sessions;
    private final OpaqueTokenPort opaqueTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public AuthenticateUserService(UserRepository repository, PasswordHasherPort passwordHasher,
                                   TokenIssuerPort tokenIssuer, BotVerificationPort botVerification,
                                   RefreshSessionRepository sessions, OpaqueTokenPort opaqueTokens,
                                   Clock clock, Duration refreshTtl) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.botVerification = botVerification;
        this.sessions = sessions;
        this.opaqueTokens = opaqueTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public CompletionStage<Result> authenticate(Command command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        return botVerification.verify(command.turnstileToken(), command.remoteIp(), "login")
                .thenCompose(valid -> valid ? repository.findByEmail(email) : failed(new BotVerificationFailed()))
                .thenCompose(user -> user.<CompletionStage<User>>map(value -> value.passwordHash() == null
                                ? failed(new InvalidCredentials())
                                : passwordHasher.matches(command.password(), value.passwordHash())
                                    .thenCompose(matches -> matches ? CompletableFuture.completedFuture(value)
                                            : failed(new InvalidCredentials())))
                        .orElseGet(() -> failed(new InvalidCredentials())))
                .thenCompose(user -> {
                    if (!user.isEmailVerified()) return failed(new EmailNotVerified());
                    TokenIssuerPort.IssuedToken token = tokenIssuer.issue(user);
                    OpaqueTokenPort.TokenPair refresh = opaqueTokens.generate();
                    Instant now = clock.instant();
                    RefreshSession session = new RefreshSession(UUID.randomUUID(), user.id(), refresh.hash(),
                            now.plus(refreshTtl), null, null, now);
                    return sessions.save(session).thenApply(ignored -> new Result(token.value(), refresh.rawValue(),
                            "Bearer", token.expiresInSeconds(), refreshTtl.toSeconds()));
                });
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }
}
