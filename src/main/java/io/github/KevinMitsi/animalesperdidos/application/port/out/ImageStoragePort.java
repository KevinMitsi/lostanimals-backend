package io.github.KevinMitsi.animalesperdidos.application.port.out;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ImageStoragePort {

    CompletionStage<String> store(UUID reportId, String fileName, String contentType, byte[] content);

    CompletionStage<Void> delete(String objectKey);
}
