package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

public record GoogleAuthenticationResponse(String accessToken, String refreshToken, String tokenType,
                                           long expiresInSeconds, long refreshExpiresInSeconds,
                                           boolean profileComplete, boolean newUser) {
}
