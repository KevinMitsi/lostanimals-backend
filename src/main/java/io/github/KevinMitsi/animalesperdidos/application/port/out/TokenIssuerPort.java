package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.User;

public interface TokenIssuerPort {
    IssuedToken issue(User user);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
