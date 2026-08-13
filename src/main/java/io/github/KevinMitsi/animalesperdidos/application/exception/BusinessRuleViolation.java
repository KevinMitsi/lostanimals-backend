package io.github.kevinmitsi.animalesperdidos.application.exception;

public class BusinessRuleViolation extends RuntimeException {
    public BusinessRuleViolation(String message) {
        super(message);
    }
}
