package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BotVerificationFailed;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserRepository repository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final BotVerificationPort botVerification;

    public AuthenticateUserService(UserRepository repository, PasswordHasherPort passwordHasher,
                                   TokenIssuerPort tokenIssuer, BotVerificationPort botVerification) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.botVerification = botVerification;
    }

    @Override
    public CompletionStage<Result> authenticate(Command command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        return botVerification.verify(command.turnstileToken(), command.remoteIp(), "login")
                .thenCompose(valid -> valid ? repository.findByEmail(email) : failed(new BotVerificationFailed()))
                .thenCompose(user -> user.<CompletionStage<User>>map(value -> passwordHasher
                                .matches(command.password(), value.passwordHash())
                                .thenCompose(matches -> matches ? CompletableFuture.completedFuture(value)
                                        : failed(new InvalidCredentials())))
                        .orElseGet(() -> failed(new InvalidCredentials())))
                .thenApply(user -> {
                    TokenIssuerPort.IssuedToken token = tokenIssuer.issue(user);
                    return new Result(token.value(), "Bearer", token.expiresInSeconds());
                });
    }

    private static <T> CompletionStage<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(error);
    }
}
