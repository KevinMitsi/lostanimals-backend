package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServicesTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final BotVerificationPort BOT = (token, ip, action) -> CompletableFuture.completedFuture(true);

    @Test
    void registersWithNormalizedEmailAndHashedPassword() {
        FakeUsers users = new FakeUsers();
        RegisterUserService service = new RegisterUserService(users, passwords(), BOT, CLOCK);

        RegisterUserUseCase.Result result = service.register(new RegisterUserUseCase.Command(
                " ANA@Example.com ", "ClaveMuySegura2026", "+573001234567", "1094912345",
                "Ana García", true, "turnstile", "127.0.0.1")).toCompletableFuture().join();

        assertEquals("ana@example.com", result.email());
        assertEquals("hash:ClaveMuySegura2026", users.user.passwordHash());
        assertEquals(NOW, users.user.habeasDataAcceptedAt());
    }

    @Test
    void rejectsDuplicatePhoneBeforeHashing() {
        FakeUsers users = new FakeUsers();
        users.phoneExists = true;

        CompletionException error = assertThrows(CompletionException.class, () ->
                new RegisterUserService(users, passwords(), BOT, CLOCK).register(new RegisterUserUseCase.Command(
                        "ana@example.com", "ClaveMuySegura2026", "+573001234567", "1094912345",
                        "Ana García", true, "turnstile", null)).toCompletableFuture().join());

        assertInstanceOf(DuplicateUserData.class, error.getCause());
        assertNull(users.user);
    }

    @Test
    void authenticatesAndIssuesBearerToken() {
        FakeUsers users = new FakeUsers();
        users.user = User.register(java.util.UUID.randomUUID(), "ana@example.com", "hash:secret",
                "+573001234567", "1094912345", "Ana", NOW);
        TokenIssuerPort tokens = user -> new TokenIssuerPort.IssuedToken("signed.jwt", 3600);

        AuthenticateUserUseCase.Result result = new AuthenticateUserService(users, passwords(), tokens, BOT)
                .authenticate(new AuthenticateUserUseCase.Command("ANA@example.com", "secret", "token", null))
                .toCompletableFuture().join();

        assertEquals("signed.jwt", result.accessToken());
        assertEquals("Bearer", result.tokenType());
    }

    @Test
    void doesNotRevealWhetherEmailExists() {
        CompletionException error = assertThrows(CompletionException.class, () ->
                new AuthenticateUserService(new FakeUsers(), passwords(), user -> null, BOT)
                        .authenticate(new AuthenticateUserUseCase.Command("nobody@example.com", "secret", "token", null))
                        .toCompletableFuture().join());
        assertInstanceOf(InvalidCredentials.class, error.getCause());
    }

    private static PasswordHasherPort passwords() {
        return new PasswordHasherPort() {
            public CompletionStage<String> hash(String raw) { return CompletableFuture.completedFuture("hash:" + raw); }
            public CompletionStage<Boolean> matches(String raw, String hash) {
                return CompletableFuture.completedFuture(hash.equals("hash:" + raw));
            }
        };
    }

    private static final class FakeUsers implements UserRepository {
        private User user;
        private boolean phoneExists;
        public CompletionStage<Boolean> existsByEmail(String email) { return CompletableFuture.completedFuture(false); }
        public CompletionStage<Boolean> existsByPhone(String phone) { return CompletableFuture.completedFuture(phoneExists); }
        public CompletionStage<Boolean> existsByDocumentNumber(String document) { return CompletableFuture.completedFuture(false); }
        public CompletionStage<Optional<User>> findByEmail(String email) { return CompletableFuture.completedFuture(Optional.ofNullable(user)); }
        public CompletionStage<User> save(User value) { user = value; return CompletableFuture.completedFuture(value); }
    }
}
