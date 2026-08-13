package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface AdminUseCase {
    CompletionStage<List<ServiceAreaView>> serviceAreas(UUID adminId);
    CompletionStage<Void> setServiceArea(UUID adminId, UUID cityId, boolean enabled);
    CompletionStage<Void> changeRole(UUID adminId, UUID userId, UserRole role);
    record ServiceAreaView(UUID cityId, String cityName, UUID departmentId, String departmentName, boolean enabled) { }
}
