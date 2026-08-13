package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.port.out.RefreshSessionRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.RefreshSession;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcRefreshSessionRepository implements RefreshSessionRepository {
    private final DatabaseClient databaseClient;

    @Override
    public CompletionStage<Void> save(RefreshSession session) {
        return databaseClient.sql("""
                INSERT INTO refresh_session(id, user_id, token_hash, expires_at, created_at)
                VALUES (:id, :userId, :hash, :expiresAt, :createdAt)
                """).bind("id", session.id()).bind("userId", session.userId()).bind("hash", session.tokenHash())
                .bind("expiresAt", session.expiresAt()).bind("createdAt", session.createdAt())
                .fetch().rowsUpdated().then().toFuture();
    }

    @Override
    public CompletionStage<Optional<RefreshSession>> rotate(String currentHash, UUID replacementId,
                                                             String replacementHash, Instant replacementExpiresAt,
                                                             Instant now) {
        return databaseClient.sql("""
                WITH current_session AS (
                    UPDATE refresh_session
                    SET revoked_at = :now, replaced_by_id = :replacementId
                    WHERE token_hash = :currentHash AND revoked_at IS NULL AND expires_at > :now
                    RETURNING id, user_id
                ), inserted AS (
                    INSERT INTO refresh_session(id, user_id, token_hash, expires_at, created_at)
                    SELECT :replacementId, user_id, :replacementHash, :replacementExpiresAt, :now
                    FROM current_session
                    RETURNING id, user_id, token_hash, expires_at, revoked_at, replaced_by_id, created_at
                )
                SELECT * FROM inserted
                """).bind("now", now).bind("replacementId", replacementId).bind("currentHash", currentHash)
                .bind("replacementHash", replacementHash).bind("replacementExpiresAt", replacementExpiresAt)
                .map((row, metadata) -> map(row)).one()
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }

    @Override
    public CompletionStage<Boolean> revoke(String tokenHash, Instant now) {
        return databaseClient.sql("""
                UPDATE refresh_session SET revoked_at=:now
                WHERE token_hash=:hash AND revoked_at IS NULL
                """).bind("now", now).bind("hash", tokenHash).fetch().rowsUpdated()
                .map(rows -> rows == 1).toFuture();
    }

    @Override
    public CompletionStage<Void> revokeAllByUser(UUID userId, Instant now) {
        return databaseClient.sql("""
                UPDATE refresh_session SET revoked_at=:now
                WHERE user_id=:userId AND revoked_at IS NULL
                """).bind("now", now).bind("userId", userId).fetch().rowsUpdated().then().toFuture();
    }

    private RefreshSession map(io.r2dbc.spi.Row row) {
        return new RefreshSession(row.get("id", UUID.class), row.get("user_id", UUID.class),
                row.get("token_hash", String.class), row.get("expires_at", Instant.class),
                row.get("revoked_at", Instant.class), row.get("replaced_by_id", UUID.class),
                row.get("created_at", Instant.class));
    }
}
