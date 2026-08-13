package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PasswordHasherPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.TokenIssuerPort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.UserRepository;
import io.github.KevinMitsi.animalesperdidos.application.service.AuthenticateUserService;
import io.github.KevinMitsi.animalesperdidos.application.service.ReportLostPetService;
import io.github.KevinMitsi.animalesperdidos.application.service.RegisterUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ReportLostPetUseCase reportLostPetUseCase(LostPetReportRepository repository,
                                               ImageStoragePort imageStorage,
                                               NotificationPort notification,
                                               Clock clock) {
        return new ReportLostPetService(repository, imageStorage, notification, clock);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository repository, PasswordHasherPort passwordHasher,
                                             BotVerificationPort botVerification, Clock clock) {
        return new RegisterUserService(repository, passwordHasher, botVerification, clock);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepository repository, PasswordHasherPort passwordHasher,
                                                     TokenIssuerPort tokenIssuer, BotVerificationPort botVerification) {
        return new AuthenticateUserService(repository, passwordHasher, tokenIssuer, botVerification);
    }
}
