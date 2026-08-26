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
                                               Clock clock, ServiceAreaRepository serviceAreas) {
        return new ReportLostPetService(repository, imageStorage, notification, clock, serviceAreas);
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
                                                           ImageStoragePort storage, Clock clock,
                                                           ServiceAreaRepository serviceAreas) {
        return new ManageLostPetReportService(repository, storage, clock, serviceAreas);
    }

    @Bean
    CreateSightingUseCase createSightingUseCase(SightingRepository repository, ImageStoragePort storage, Clock clock,
                                                ServiceAreaRepository serviceAreas) {
        return new CreateSightingService(repository, storage, clock, serviceAreas);
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
    ManageSightingUseCase manageSightingUseCase(SightingRepository repository, ImageStoragePort storage, Clock clock,
                                                ServiceAreaRepository serviceAreas) {
        return new ManageSightingService(repository, storage, clock, serviceAreas);
    }

    @Bean ContactRequestUseCase contactRequestUseCase(ContactRepository contacts, LostPetReportRepository reports,
                                                       SightingRepository sightings, Clock clock) {
        return new ContactRequestService(contacts, reports, sightings, clock);
    }

    @Bean ConversationUseCase conversationUseCase(ContactRepository contacts, UserRepository users,
                                                   MessageEventPublisher messageEvents, Clock clock) {
        return new ConversationService(contacts, users, messageEvents, clock);
    }

    @Bean ReunionModerationUseCase reunionModerationUseCase(ModerationRepository moderation,
            LostPetReportRepository reports, UserRepository users, Clock clock) {
        return new ReunionModerationService(moderation, reports, users, clock);
    }

    @Bean AdminUseCase adminUseCase(ServiceAreaRepository areas, UserRepository users, Clock clock) {
        return new AdminService(areas, users, clock);
    }

    @Bean ContentModerationUseCase contentModerationUseCase(ModerationRepository moderation, UserRepository users, Clock clock) {
        return new ContentModerationService(moderation, users, clock);
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
    GoogleAuthenticationUseCase googleAuthenticationUseCase(UserRepository repository, GoogleIdentityPort google,
                                                             TokenIssuerPort tokenIssuer,
                                                             RefreshSessionRepository sessions,
                                                             OpaqueTokenPort opaqueTokens, Clock clock,
                                                             SecurityProperties properties) {
        return new GoogleAuthenticationService(repository, google, tokenIssuer, sessions, opaqueTokens,
                clock, properties.getRefreshTtl());
    }

    @Bean
    CompleteGoogleProfileUseCase completeGoogleProfileUseCase(UserRepository users) {
        return new CompleteGoogleProfileService(users);
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

    @Bean ManagePushSubscriptionUseCase managePushSubscriptionUseCase(PushSubscriptionPort subscriptions) {
        return new ManagePushSubscriptionService(subscriptions);
    }
}
