package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.User;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface UserRepository {
    CompletionStage<Boolean> existsByEmail(String email);
    CompletionStage<Boolean> existsByPhone(String phone);
    CompletionStage<Boolean> existsByDocumentNumber(String documentNumber);
    CompletionStage<Optional<User>> findByEmail(String email);
    CompletionStage<Optional<User>> findById(UUID id);
    CompletionStage<User> save(User user);
    CompletionStage<User> update(User user);
}
