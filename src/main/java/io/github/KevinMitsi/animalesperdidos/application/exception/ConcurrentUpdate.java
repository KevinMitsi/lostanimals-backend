package io.github.KevinMitsi.animalesperdidos.application.exception;

public class ConcurrentUpdate extends RuntimeException {
    public ConcurrentUpdate() { super("The report was modified by another request"); }
}
