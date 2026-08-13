package io.github.KevinMitsi.animalesperdidos.application.port.in;

import io.github.KevinMitsi.animalesperdidos.domain.model.Species;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ReportLostPetUseCase {

    CompletionStage<Result> report(Command command);

    record Command(UUID ownerId, String petName, Species species, String description,
                   Instant disappearedAt, double latitude, double longitude,
                   UUID neighborhoodId, List<Image> images) {
        public Command {
            images = List.copyOf(images);
        }
    }

    record Image(String fileName, String contentType, byte[] content) {
        public Image {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    record Result(UUID reportId) {
    }
}
