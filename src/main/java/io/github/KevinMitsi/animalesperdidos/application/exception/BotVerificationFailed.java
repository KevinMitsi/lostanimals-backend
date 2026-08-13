package io.github.KevinMitsi.animalesperdidos.application.exception;

public class BotVerificationFailed extends RuntimeException {
    public BotVerificationFailed() {
        super("Bot verification failed");
    }
}
