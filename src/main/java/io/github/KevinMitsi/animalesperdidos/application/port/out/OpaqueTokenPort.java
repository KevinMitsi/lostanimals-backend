package io.github.KevinMitsi.animalesperdidos.application.port.out;

public interface OpaqueTokenPort {
    TokenPair generate();
    String hash(String rawToken);

    record TokenPair(String rawValue, String hash) { }
}
