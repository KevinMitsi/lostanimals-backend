package io.github.KevinMitsi.animalesperdidos.application.exception;

public class DuplicateUserData extends RuntimeException {
    public DuplicateUserData(String field) {
        super("A user with the same " + field + " already exists");
    }
}
