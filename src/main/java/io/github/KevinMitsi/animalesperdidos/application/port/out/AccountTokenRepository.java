package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface AccountTokenRepository {
    CompletionStage<Void> replaceActive(AccountToken token);
    CompletionStage<Optional<AccountToken>> consume(String tokenHash, AccountToken.Type type, Instant now);
    CompletionStage<Void> consumeAll(UUID userId, AccountToken.Type type, Instant now);
}
