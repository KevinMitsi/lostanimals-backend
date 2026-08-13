package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.application.service.*;
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
    PrepareReportImageUploadUseCase prepareReportImageUploadUseCase(ImageStoragePort storage) {
        return new PrepareReportImageUploadService(storage);
    }

    @Bean
    QueryLostPetReportsUseCase queryLostPetReportsUseCase(LostPetReportRepository repository,
                                                           ImageStoragePort storage) {
        return new QueryLostPetReportsService(repository, storage);
    }

    @Bean
    ManageLostPetReportUseCase manageLostPetReportUseCase(LostPetReportRepository repository,
                                                           ImageStoragePort storage, Clock clock) {
        return new ManageLostPetReportService(repository, storage, clock);
    }

    @Bean
    CreateSightingUseCase createSightingUseCase(SightingRepository repository, ImageStoragePort storage, Clock clock) {
        return new CreateSightingService(repository, storage, clock);
    }

    @Bean
    PrepareSightingImageUploadUseCase prepareSightingImageUploadUseCase(ImageStoragePort storage) {
        return new PrepareSightingImageUploadService(storage);
    }

    @Bean
    QuerySightingsUseCase querySightingsUseCase(SightingRepository repository, ImageStoragePort storage) {
        return new QuerySightingsService(repository, storage);
    }

    @Bean
    ManageSightingUseCase manageSightingUseCase(SightingRepository repository, ImageStoragePort storage, Clock clock) {
        return new ManageSightingService(repository, storage, clock);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository repository, PasswordHasherPort passwordHasher,
                                             BotVerificationPort botVerification, Clock clock,
                                             AccountTokenRepository accountTokens, OpaqueTokenPort opaqueTokens,
                                             AccountNotificationPort notifications, SecurityProperties properties) {
        return new RegisterUserService(repository, passwordHasher, botVerification, clock, accountTokens,
                opaqueTokens, notifications, properties.getEmailVerificationTtl());
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepository repository, PasswordHasherPort passwordHasher,
                                                     TokenIssuerPort tokenIssuer, BotVerificationPort botVerification,
                                                     RefreshSessionRepository sessions, OpaqueTokenPort opaqueTokens,
                                                     Clock clock, SecurityProperties properties) {
        return new AuthenticateUserService(repository, passwordHasher, tokenIssuer, botVerification,
                sessions, opaqueTokens, clock, properties.getRefreshTtl());
    }

    @Bean
    AccountLifecycleService accountLifecycleService(UserRepository users, AccountTokenRepository tokens,
                                                     RefreshSessionRepository sessions, OpaqueTokenPort opaqueTokens,
                                                     PasswordHasherPort passwordHasher, AccountNotificationPort notifications,
                                                     Clock clock, SecurityProperties properties,
                                                     BotVerificationPort botVerification) {
        return new AccountLifecycleService(users, tokens, sessions, opaqueTokens, passwordHasher, notifications,
                clock, properties.getEmailVerificationTtl(), properties.getPasswordResetTtl(), botVerification);
    }

    @Bean
    RefreshSessionUseCase refreshSessionUseCase(RefreshSessionRepository sessions, UserRepository users,
                                                 OpaqueTokenPort opaqueTokens, TokenIssuerPort accessTokens,
                                                 Clock clock, SecurityProperties properties) {
        return new RefreshSessionService(sessions, users, opaqueTokens, accessTokens, clock, properties.getRefreshTtl());
    }
}
