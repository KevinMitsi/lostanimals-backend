package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.util.*;

public record PublicationRef(PublicationType type, UUID id) {
    public PublicationRef { Objects.requireNonNull(type); Objects.requireNonNull(id); }
}
