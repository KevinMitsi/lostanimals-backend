package io.github.kevinmitsi.animalesperdidos.infrastructure.config;

import io.github.kevinmitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.kevinmitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.kevinmitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.kevinmitsi.animalesperdidos.application.port.out.NotificationPort;
import io.github.kevinmitsi.animalesperdidos.application.service.ReportLostPetService;
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
}
