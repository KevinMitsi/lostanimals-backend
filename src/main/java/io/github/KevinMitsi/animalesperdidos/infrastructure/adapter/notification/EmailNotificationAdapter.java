package io.github.kevinmitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.kevinmitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.kevinmitsi.animalesperdidos.domain.model.LostPetReport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class EmailNotificationAdapter implements NotificationPort {
    private final JavaMailSender mailSender;
    private final DatabaseClient databaseClient;

    @Override
    public CompletionStage<Void> reportCreated(LostPetReport report) {
        return databaseClient.sql("SELECT email FROM app_user WHERE id = :ownerId")
                .bind("ownerId", report.ownerId())
                .map((row, metadata) -> row.get("email", String.class))
                .one()
                .flatMap(email -> send(email, report))
                .then()
                .toFuture();
    }

    private Mono<Void> send(String recipient, LostPetReport report) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(recipient);
                    message.setSubject("Reporte de mascota publicado");
                    message.setText("El reporte de " + report.petName() + " fue publicado correctamente.");
                    mailSender.send(message);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
