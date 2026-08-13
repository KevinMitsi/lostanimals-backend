package io.github.KevinMitsi.animalesperdidos.application.exception;

public class ResourceNotFound extends RuntimeException {
    public ResourceNotFound(String resource) { super(resource + " was not found"); }
}
