package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.concurrent.CompletionStage;

public interface PasswordHasherPort {
    CompletionStage<String> hash(String rawPassword);
    CompletionStage<Boolean> matches(String rawPassword, String passwordHash);
}
