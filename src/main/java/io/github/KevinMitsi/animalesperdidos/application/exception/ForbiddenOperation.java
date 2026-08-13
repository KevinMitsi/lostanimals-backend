package io.github.KevinMitsi.animalesperdidos.application.exception;

public class ForbiddenOperation extends RuntimeException {
    public ForbiddenOperation() { super("You are not allowed to modify this resource"); }
}
