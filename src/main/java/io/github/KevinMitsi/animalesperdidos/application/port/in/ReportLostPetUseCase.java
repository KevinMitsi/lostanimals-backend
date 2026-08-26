package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import io.github.KevinMitsi.animalesperdidos.domain.model.AdministrativeLocation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ReportLostPetUseCase {

    CompletionStage<Result> report(Command command);

    record Command(UUID ownerId, String petName, Species species, String description,
                   Instant disappearedAt, double latitude, double longitude,
                   AdministrativeLocation administrativeLocation, List<String> imageKeys) {
        public Command {
            imageKeys = List.copyOf(imageKeys);
        }
    }

    record Result(UUID reportId) {
    }
}
