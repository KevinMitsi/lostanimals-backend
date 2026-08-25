package io.github.KevinMitsi.animalesperdidos.application.port.out;

import io.github.KevinMitsi.animalesperdidos.domain.model.Message;

import java.util.concurrent.CompletionStage;

public interface MessageEventPublisher {
    CompletionStage<Void> publish(Message message);
}
