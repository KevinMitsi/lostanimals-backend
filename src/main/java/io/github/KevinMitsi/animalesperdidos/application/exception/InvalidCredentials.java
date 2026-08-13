package io.github.KevinMitsi.animalesperdidos.application.exception;

public class InvalidCredentials extends RuntimeException {
    public InvalidCredentials() {
        super("Invalid email or password");
    }
}
