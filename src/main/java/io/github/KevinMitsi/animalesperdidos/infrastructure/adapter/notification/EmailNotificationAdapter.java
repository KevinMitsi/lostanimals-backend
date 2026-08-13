package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.notification;

import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.AccountNotificationPort;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class EmailNotificationAdapter implements NotificationPort, AccountNotificationPort {
    private final JavaMailSender mailSender;
    private final DatabaseClient databaseClient;
    @Value("${app.notifications.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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

    @Override
    public CompletionStage<Void> sendEmailVerification(String email, String displayName, String rawToken) {
        return sendAccountEmail(email, "Verifica tu correo",
                "Hola " + displayName + ", verifica tu correo en: " + frontendBaseUrl + "/verify-email?token=" + rawToken);
    }

    @Override
    public CompletionStage<Void> sendPasswordReset(String email, String displayName, String rawToken) {
        return sendAccountEmail(email, "Recupera tu contraseña",
                "Hola " + displayName + ", cambia tu contraseña en: " + frontendBaseUrl + "/reset-password?token=" + rawToken);
    }

    private CompletionStage<Void> sendAccountEmail(String recipient, String subject, String body) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(recipient);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                }).subscribeOn(Schedulers.boundedElastic()).then().toFuture();
    }
}
