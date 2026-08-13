package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BotVerificationFailed;
import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.AccountNotificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.AccountTokenRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.OpaqueTokenPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class RegisterUserService implements RegisterUserUseCase {
    private final UserRepository repository;
    private final PasswordHasherPort passwordHasher;
    private final BotVerificationPort botVerification;
    private final Clock clock;
    private final AccountTokenRepository accountTokens;
    private final OpaqueTokenPort opaqueTokens;
    private final AccountNotificationPort notifications;
    private final Duration verificationTtl;

    public RegisterUserService(UserRepository repository, PasswordHasherPort passwordHasher,
                               BotVerificationPort botVerification, Clock clock,
                               AccountTokenRepository accountTokens, OpaqueTokenPort opaqueTokens,
                               AccountNotificationPort notifications, Duration verificationTtl) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.botVerification = botVerification;
        this.clock = clock;
        this.accountTokens = accountTokens;
        this.opaqueTokens = opaqueTokens;
        this.notifications = notifications;
        this.verificationTtl = verificationTtl;
    }

    @Override
    public CompletionStage<Result> register(Command command) {
        if (!command.acceptsDataProcessing()) {
            return CompletableFuture.failedFuture(new BusinessRuleViolation("Data processing consent is required"));
        }
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        return botVerification.verify(command.turnstileToken(), command.remoteIp(), "register")
                .thenCompose(valid -> valid ? checkUniqueness(email, command) : failed(new BotVerificationFailed()))
                .thenCompose(ignored -> passwordHasher.hash(command.password()))
                .thenCompose(hash -> repository.save(User.register(UUID.randomUUID(), email, hash,
                        command.phone(), command.documentNumber(), command.displayName(), clock.instant())))
                .thenCompose(this::createVerification)
                .thenApply(user -> new Result(user.id(), user.email()));
    }

    private CompletionStage<User> createVerification(User user) {
        Instant now = clock.instant();
        OpaqueTokenPort.TokenPair pair = opaqueTokens.generate();
        AccountToken token = new AccountToken(UUID.randomUUID(), user.id(), AccountToken.Type.EMAIL_VERIFICATION,
                pair.hash(), now.plus(verificationTtl), null, now);
        return accountTokens.replaceActive(token)
                .thenCompose(ignored -> notifications.sendEmailVerification(user.email(), user.displayName(), pair.rawValue())
                        .exceptionally(error -> null))
                .thenApply(ignored -> user);
    }

    private CompletionStage<Void> checkUniqueness(String email, Command command) {
        CompletionStage<Boolean> emailExists = repository.existsByEmail(email);
        CompletionStage<Boolean> phoneExists = repository.existsByPhone(command.phone());
        CompletionStage<Boolean> documentExists = repository.existsByDocumentNumber(command.documentNumber());
        return emailExists.thenCombine(phoneExists, Checks::new)
                .thenCombine(documentExists, (checks, document) -> new Checks(checks.email(), checks.phone(), document))
                .thenCompose(checks -> {
                    if (checks.email()) return failed(new DuplicateUserData("email"));
                    if (checks.phone()) return failed(new DuplicateUserData("phone"));
                    if (checks.document()) return failed(new DuplicateUserData("document number"));
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }

    private record Checks(boolean email, boolean phone, boolean document) {
        Checks(boolean email, boolean phone) {
            this(email, phone, false);
        }
    }
}
