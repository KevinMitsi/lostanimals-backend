package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties("app.security.jwt")
public class SecurityProperties {
    private String secret;
    private String issuer = "animales-perdidos-colombia";
    private Duration ttl = Duration.ofHours(1);
    private Duration refreshTtl = Duration.ofDays(30);
    private Duration emailVerificationTtl = Duration.ofHours(24);
    private Duration passwordResetTtl = Duration.ofMinutes(30);

}
