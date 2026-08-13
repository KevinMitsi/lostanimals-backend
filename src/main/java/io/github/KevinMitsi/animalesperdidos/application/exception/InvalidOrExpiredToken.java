package io.github.KevinMitsi.animalesperdidos.application.exception;

public class InvalidOrExpiredToken extends RuntimeException {
    public InvalidOrExpiredToken() { super("Token is invalid, expired or already used"); }
}
