package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.port.out.GoogleIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class GoogleIdentityAdapter implements GoogleIdentityPort {
    private final GoogleIdTokenVerifier verifier;

    @Override
    public CompletionStage<Identity> verify(String credential) {
        return Mono.fromCallable(() -> verifier.verify(credential))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token -> token == null ? Mono.error(new InvalidCredentials()) : Mono.just(toIdentity(token)))
                .onErrorMap(error -> error instanceof InvalidCredentials ? error : new InvalidCredentials())
                .toFuture();
    }

    private Identity toIdentity(GoogleIdToken token) {
        GoogleIdToken.Payload payload = token.getPayload();
        return new Identity(payload.getSubject(), payload.getEmail(), Boolean.TRUE.equals(payload.getEmailVerified()),
                stringClaim(payload, "name"), stringClaim(payload, "picture"));
    }

    private static String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value == null ? null : value.toString();
    }
}
