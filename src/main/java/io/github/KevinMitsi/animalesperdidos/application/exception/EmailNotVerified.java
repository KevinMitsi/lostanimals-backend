package io.github.KevinMitsi.animalesperdidos.application.exception;

public class EmailNotVerified extends RuntimeException {
    public EmailNotVerified() { super("Email verification is required"); }
}
