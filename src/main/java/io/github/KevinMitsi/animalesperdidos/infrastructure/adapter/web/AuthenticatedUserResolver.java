package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class AuthenticatedUserResolver {
    public UUID id(Jwt jwt) { return UUID.fromString(Objects.requireNonNull(jwt.getSubject())); }
}
