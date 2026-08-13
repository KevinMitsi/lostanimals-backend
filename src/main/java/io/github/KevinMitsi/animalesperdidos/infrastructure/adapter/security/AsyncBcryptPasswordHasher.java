package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security;

import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class AsyncBcryptPasswordHasher implements PasswordHasherPort {
    private final PasswordEncoder encoder;

    @Override
    public CompletionStage<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> encoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic()).toFuture();
    }

    @Override
    public CompletionStage<Boolean> matches(String rawPassword, String passwordHash) {
        return Mono.fromCallable(() -> encoder.matches(rawPassword, passwordHash))
                .subscribeOn(Schedulers.boundedElastic()).toFuture();
    }
}
