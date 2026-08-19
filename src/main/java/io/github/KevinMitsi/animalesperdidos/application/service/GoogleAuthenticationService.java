package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.in.GoogleAuthenticationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.GoogleIdentityPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.OpaqueTokenPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.RefreshSessionRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class GoogleAuthenticationService implements GoogleAuthenticationUseCase {
    private final UserRepository users;
    private final GoogleIdentityPort google;
    private final TokenIssuerPort accessTokens;
    private final RefreshSessionRepository sessions;
    private final OpaqueTokenPort opaqueTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public GoogleAuthenticationService(UserRepository users, GoogleIdentityPort google,
                                       TokenIssuerPort accessTokens, RefreshSessionRepository sessions,
                                       OpaqueTokenPort opaqueTokens, Clock clock, Duration refreshTtl) {
        this.users = users;
        this.google = google;
        this.accessTokens = accessTokens;
        this.sessions = sessions;
        this.opaqueTokens = opaqueTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public CompletionStage<Result> authenticate(Command command) {
        return google.verify(command.credential())
                .thenCompose(identity -> validate(identity).thenCompose(ignored -> findOrCreate(identity, command)))
                .thenCompose(account -> issueSession(account.user(), account.created()));
    }

    private CompletionStage<Account> findOrCreate(GoogleIdentityPort.Identity identity, Command command) {
        return users.findByGoogleSubject(identity.subject()).thenCompose(bySubject -> bySubject
                .<CompletionStage<Account>>map(user -> completed(new Account(user, false)))
                .orElseGet(() -> users.findByEmail(identity.email().trim().toLowerCase(Locale.ROOT))
                        .thenCompose(byEmail -> byEmail
                                .<CompletionStage<Account>>map(user -> linkExisting(user, identity))
                                .orElseGet(() -> create(identity, command.acceptsDataProcessing())))));
    }

    private CompletionStage<Account> linkExisting(User user, GoogleIdentityPort.Identity identity) {
        if (user.googleSubject() != null && !user.googleSubject().equals(identity.subject())) {
            return failed(new InvalidCredentials());
        }
        User linked = user.isEmailVerified() ? user.linkGoogle(identity.subject(), identity.pictureUrl())
                : user.verifyEmail(clock.instant()).linkGoogle(identity.subject(), identity.pictureUrl());
        return users.update(linked).thenApply(saved -> new Account(saved, false));
    }

    private CompletionStage<Account> create(GoogleIdentityPort.Identity identity, boolean acceptsDataProcessing) {
        if (!acceptsDataProcessing) {
            return failed(new BusinessRuleViolation(
                    "Data processing consent is required to create an account with Google"));
        }
        String displayName = identity.displayName() == null || identity.displayName().isBlank()
                ? identity.email().substring(0, identity.email().indexOf('@')) : identity.displayName();
        User user = User.registerWithGoogle(UUID.randomUUID(), identity.email(), displayName,
                identity.subject(), identity.pictureUrl(), clock.instant());
        return users.save(user).thenApply(saved -> new Account(saved, true));
    }

    private CompletionStage<Void> validate(GoogleIdentityPort.Identity identity) {
        if (!identity.emailVerified() || identity.subject() == null || identity.subject().isBlank()
                || identity.email() == null || identity.email().isBlank() || !identity.email().contains("@")) {
            return failed(new InvalidCredentials());
        }
        return completed(null);
    }

    private CompletionStage<Result> issueSession(User user, boolean created) {
        TokenIssuerPort.IssuedToken access = accessTokens.issue(user);
        OpaqueTokenPort.TokenPair refresh = opaqueTokens.generate();
        Instant now = clock.instant();
        RefreshSession session = new RefreshSession(UUID.randomUUID(), user.id(), refresh.hash(),
                now.plus(refreshTtl), null, null, now);
        return sessions.save(session).thenApply(ignored -> new Result(access.value(), refresh.rawValue(),
                "Bearer", access.expiresInSeconds(), refreshTtl.toSeconds(), user.isProfileComplete(), created));
    }

    private static <T> CompletionStage<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }

    private record Account(User user, boolean created) {
    }
}
