package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ServiceAreaRepository {
    CompletionStage<Boolean> isMunicipalityEnabled(String municipalityCode);
    CompletionStage<List<AreaEntry>> list();
    CompletionStage<Void> setEnabled(String municipalityCode, boolean enabled, UUID actorId, Instant now);
    record AreaEntry(String municipalityCode, boolean enabled) { }
}
