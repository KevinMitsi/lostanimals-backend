package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidOrExpiredToken;
import io.github.KevinMitsi.animalesperdidos.application.exception.BotVerificationFailed;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PasswordRecoveryUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.VerifyEmailUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class AccountLifecycleService implements VerifyEmailUseCase, PasswordRecoveryUseCase {
    private final UserRepository users;
    private final AccountTokenRepository tokens;
    private final RefreshSessionRepository sessions;
    private final OpaqueTokenPort opaqueTokens;
    private final PasswordHasherPort passwordHasher;
    private final AccountNotificationPort notifications;
    private final Clock clock;
    private final Duration verificationTtl;
    private final Duration resetTtl;
    private final BotVerificationPort botVerification;

    public AccountLifecycleService(UserRepository users, AccountTokenRepository tokens,
                                   RefreshSessionRepository sessions, OpaqueTokenPort opaqueTokens,
                                   PasswordHasherPort passwordHasher, AccountNotificationPort notifications,
                                   Clock clock, Duration verificationTtl, Duration resetTtl,
                                   BotVerificationPort botVerification) {
        this.users = users;
        this.tokens = tokens;
        this.sessions = sessions;
        this.opaqueTokens = opaqueTokens;
        this.passwordHasher = passwordHasher;
        this.notifications = notifications;
        this.clock = clock;
        this.verificationTtl = verificationTtl;
        this.resetTtl = resetTtl;
        this.botVerification = botVerification;
    }

    @Override
    public CompletionStage<Void> verify(String rawToken) {
        Instant now = clock.instant();
        return tokens.consume(opaqueTokens.hash(rawToken), AccountToken.Type.EMAIL_VERIFICATION, now)
                .thenCompose(optional -> optional.map(accountToken -> users.findById(accountToken.userId()).thenCompose(user -> user.<CompletionStage<Void>>map(value -> users.update(value.verifyEmail(now)).thenApply(ignored -> null)).orElseGet(() -> failed(new InvalidOrExpiredToken())))).orElseGet(() -> failed(new InvalidOrExpiredToken())));
    }

    @Override
    public CompletionStage<Void> resend(String email, String turnstileToken, String remoteIp) {
        return requireHuman(turnstileToken, remoteIp, "resend-verification")
                .thenCompose(ignored -> users.findByEmail(normalize(email))).thenCompose(optional -> optional
                .filter(user -> !user.isEmailVerified())
                .map(this::sendVerification)
                .orElseGet(() -> CompletableFuture.completedFuture(null)));
    }

    @Override
    public CompletionStage<Void> request(String email, String turnstileToken, String remoteIp) {
        return requireHuman(turnstileToken, remoteIp, "password-recovery")
                .thenCompose(ignored -> users.findByEmail(normalize(email))).thenCompose(optional -> optional
                .map(this::sendPasswordReset)
                .orElseGet(() -> CompletableFuture.completedFuture(null)));
    }

    @Override
    public CompletionStage<Void> reset(String rawToken, String newPassword) {
        Instant now = clock.instant();
        return tokens.consume(opaqueTokens.hash(rawToken), AccountToken.Type.PASSWORD_RESET, now)
                .thenCompose(optional -> {
                    if (optional.isEmpty()) return failed(new InvalidOrExpiredToken());
                    UUID userId = optional.get().userId();
                    return passwordHasher.hash(newPassword)
                            .thenCompose(hash -> users.findById(userId).thenCompose(user -> {
                                if (user.isEmpty()) return failed(new InvalidOrExpiredToken());
                                return users.update(user.get().changePassword(hash));
                            }))
                            .thenCompose(user -> sessions.revokeAllByUser(user.id(), now));
                });
    }

    private CompletionStage<Void> sendVerification(User user) {
        return createToken(user, AccountToken.Type.EMAIL_VERIFICATION, verificationTtl)
                .thenCompose(pair -> notifications.sendEmailVerification(user.email(), user.displayName(), pair.rawValue())
                        .exceptionally(error -> null));
    }

    private CompletionStage<Void> sendPasswordReset(User user) {
        return createToken(user, AccountToken.Type.PASSWORD_RESET, resetTtl)
                .thenCompose(pair -> notifications.sendPasswordReset(user.email(), user.displayName(), pair.rawValue())
                        .exceptionally(error -> null));
    }

    private CompletionStage<OpaqueTokenPort.TokenPair> createToken(User user, AccountToken.Type type, Duration ttl) {
        Instant now = clock.instant();
        OpaqueTokenPort.TokenPair pair = opaqueTokens.generate();
        AccountToken token = new AccountToken(UUID.randomUUID(), user.id(), type, pair.hash(),
                now.plus(ttl), null, now);
        return tokens.replaceActive(token).thenApply(ignored -> pair);
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private CompletionStage<Void> requireHuman(String token, String ip, String action) {
        return botVerification.verify(token, ip, action)
                .thenCompose(valid -> valid ? CompletableFuture.completedFuture(null) : failed(new BotVerificationFailed()));
    }
    private static <T> CompletionStage<T> failed(Throwable error) { return CompletableFuture.failedFuture(error); }
}
