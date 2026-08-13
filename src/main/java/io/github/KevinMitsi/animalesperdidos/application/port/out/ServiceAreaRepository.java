package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface ServiceAreaRepository {
    CompletionStage<Boolean> isNeighborhoodEnabled(UUID neighborhoodId);
    CompletionStage<List<AreaEntry>> list();
    CompletionStage<Void> setEnabled(UUID cityId, boolean enabled, UUID actorId, Instant now);
    record AreaEntry(UUID cityId, String cityName, UUID departmentId, String departmentName, boolean enabled) { }
}
