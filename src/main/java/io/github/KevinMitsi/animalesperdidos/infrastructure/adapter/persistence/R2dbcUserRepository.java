package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.entity.UserEntity;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.mapper.UserPersistenceMapper;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security.PersonalDataCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Repository
@RequiredArgsConstructor
public class R2dbcUserRepository implements UserRepository {
    private final DatabaseClient databaseClient;
    private final UserPersistenceMapper mapper;
    private final PersonalDataCipher personalDataCipher;

    @Override
    public CompletionStage<Boolean> existsByEmail(String email) {
        return exists("SELECT EXISTS(SELECT 1 FROM app_user WHERE lower(email) = lower(:value)) AS present", email);
    }

    @Override
    public CompletionStage<Boolean> existsByPhone(String phone) {
        return exists("SELECT EXISTS(SELECT 1 FROM app_user WHERE phone_lookup = :value) AS present",
                personalDataCipher.phoneLookup(phone));
    }

    @Override
    public CompletionStage<Boolean> existsByDocumentNumber(String documentNumber) {
        return exists("SELECT EXISTS(SELECT 1 FROM app_user WHERE document_number_lookup = :value) AS present",
                personalDataCipher.documentNumberLookup(documentNumber));
    }

    @Override
    public CompletionStage<Optional<User>> findByEmail(String email) {
        return selectUser("WHERE lower(email) = lower(:value)", email)
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }

    @Override
    public CompletionStage<Optional<User>> findByGoogleSubject(String subject) {
        return selectUser("WHERE google_subject = :value", subject)
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }

    @Override
    public CompletionStage<Optional<User>> findById(UUID id) {
        return selectUser("WHERE id = :value", id)
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }

    private <T> Mono<User> selectUser(String where, T value) {
        return databaseClient.sql("""
                        SELECT id, email, password_hash, phone, document_number, display_name,
                               role, habeas_data_accepted_at, email_verified_at, google_subject, picture_url, created_at
                        FROM app_user %s
                        """.formatted(where))
                .bind("value", value)
                .map((row, metadata) -> new UserEntity(
                        row.get("id", UUID.class), row.get("email", String.class),
                        row.get("password_hash", String.class), personalDataCipher.decryptPhone(row.get("phone", String.class)),
                        personalDataCipher.decryptDocumentNumber(row.get("document_number", String.class)), row.get("display_name", String.class),
                        UserRole.valueOf(row.get("role", String.class)),
                        row.get("habeas_data_accepted_at", Instant.class), row.get("email_verified_at", Instant.class),
                        row.get("google_subject", String.class), row.get("picture_url", String.class),
                        row.get("created_at", Instant.class)))
                .one().map(mapper::toDomain);
    }

    @Override
    public CompletionStage<User> save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return databaseClient.sql("""
                        INSERT INTO app_user
                            (id, email, password_hash, phone, phone_lookup, document_number, document_number_lookup, display_name,
                             habeas_data_accepted_at, email_verified_at, google_subject, picture_url, created_at)
                        VALUES (:id, :email, :passwordHash, :phone, :phoneLookup, :documentNumber, :documentLookup, :displayName,
                                :acceptedAt, :emailVerifiedAt, :googleSubject, :pictureUrl, :createdAt)
                        """)
                .bind("id", entity.id())
                .bind("email", entity.email().toLowerCase(Locale.ROOT))
                .bind("passwordHash", nullable(entity.passwordHash(), String.class))
                .bind("phone", nullable(personalDataCipher.encryptPhone(entity.phone()), String.class))
                .bind("phoneLookup", nullable(personalDataCipher.phoneLookup(entity.phone()), String.class))
                .bind("documentNumber", nullable(personalDataCipher.encryptDocumentNumber(entity.documentNumber()), String.class))
                .bind("documentLookup", nullable(personalDataCipher.documentNumberLookup(entity.documentNumber()), String.class))
                .bind("displayName", entity.displayName())
                .bind("acceptedAt", entity.habeasDataAcceptedAt())
                .bind("emailVerifiedAt", nullable(entity.emailVerifiedAt(), Instant.class))
                .bind("googleSubject", nullable(entity.googleSubject(), String.class))
                .bind("pictureUrl", nullable(entity.pictureUrl(), String.class))
                .bind("createdAt", entity.createdAt())
                .fetch().rowsUpdated().thenReturn(user)
                .onErrorMap(DataIntegrityViolationException.class,
                        ignored -> new DuplicateUserData("email, phone or document number"))
                .toFuture();
    }

    @Override
    public CompletionStage<User> update(User user) {
        return databaseClient.sql("""
                        UPDATE app_user SET password_hash = :passwordHash, phone = :phone, phone_lookup = :phoneLookup,
                            document_number = :documentNumber, document_number_lookup = :documentLookup, display_name = :displayName,
                            email_verified_at = :emailVerifiedAt, google_subject = :googleSubject,
                            picture_url = :pictureUrl, role = :role
                        WHERE id = :id
                        """)
                .bind("passwordHash", nullable(user.passwordHash(), String.class))
                .bind("phone", nullable(personalDataCipher.encryptPhone(user.phone()), String.class))
                .bind("phoneLookup", nullable(personalDataCipher.phoneLookup(user.phone()), String.class))
                .bind("documentNumber", nullable(personalDataCipher.encryptDocumentNumber(user.documentNumber()), String.class))
                .bind("documentLookup", nullable(personalDataCipher.documentNumberLookup(user.documentNumber()), String.class))
                .bind("displayName", user.displayName())
                .bind("emailVerifiedAt", nullable(user.emailVerifiedAt(), Instant.class))
                .bind("googleSubject", nullable(user.googleSubject(), String.class))
                .bind("pictureUrl", nullable(user.pictureUrl(), String.class))
                .bind("role", user.role().name())
                .bind("id", user.id())
                .fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? Mono.just(user) : Mono.error(new IllegalStateException("User not found")))
                .onErrorMap(DataIntegrityViolationException.class,
                        ignored -> new DuplicateUserData("Google identity, phone or document number"))
                .toFuture();
    }

    private CompletionStage<Boolean> exists(String sql, String value) {
        return databaseClient.sql(sql).bind("value", value)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("present", Boolean.class)))
                .one().defaultIfEmpty(false).toFuture();
    }

    private static <T> Object nullable(T value, Class<T> type) {
        return value == null ? io.r2dbc.spi.Parameters.in(type) : value;
    }
}
