package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.port.out.AccountTokenRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.AccountToken;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcAccountTokenRepository implements AccountTokenRepository {
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transaction;

    @Override
    public CompletionStage<Void> replaceActive(AccountToken token) {
        Mono<Long> expireOld = databaseClient.sql("""
                UPDATE account_token SET consumed_at = :now
                WHERE user_id = :userId AND type = :type AND consumed_at IS NULL
                """).bind("now", token.createdAt()).bind("userId", token.userId())
                .bind("type", token.type().name()).fetch().rowsUpdated();
        Mono<Long> insert = databaseClient.sql("""
                INSERT INTO account_token(id, user_id, type, token_hash, expires_at, created_at)
                VALUES (:id, :userId, :type, :hash, :expiresAt, :createdAt)
                """).bind("id", token.id()).bind("userId", token.userId()).bind("type", token.type().name())
                .bind("hash", token.tokenHash()).bind("expiresAt", token.expiresAt())
                .bind("createdAt", token.createdAt()).fetch().rowsUpdated();
        return transaction.transactional(expireOld.then(insert).then()).toFuture();
    }

    @Override
    public CompletionStage<Optional<AccountToken>> consume(String tokenHash, AccountToken.Type type, Instant now) {
        return databaseClient.sql("""
                UPDATE account_token SET consumed_at = :now
                WHERE token_hash = :hash AND type = :type AND consumed_at IS NULL AND expires_at > :now
                RETURNING id, user_id, type, token_hash, expires_at, consumed_at, created_at
                """).bind("now", now).bind("hash", tokenHash).bind("type", type.name())
                .map((row, metadata) -> new AccountToken(row.get("id", UUID.class), row.get("user_id", UUID.class),
                        AccountToken.Type.valueOf(row.get("type", String.class)), row.get("token_hash", String.class),
                        row.get("expires_at", Instant.class), row.get("consumed_at", Instant.class),
                        row.get("created_at", Instant.class)))
                .one().map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }

    @Override
    public CompletionStage<Void> consumeAll(UUID userId, AccountToken.Type type, Instant now) {
        return databaseClient.sql("UPDATE account_token SET consumed_at=:now WHERE user_id=:userId AND type=:type AND consumed_at IS NULL")
                .bind("now", now).bind("userId", userId).bind("type", type.name())
                .fetch().rowsUpdated().then().toFuture();
    }
}
