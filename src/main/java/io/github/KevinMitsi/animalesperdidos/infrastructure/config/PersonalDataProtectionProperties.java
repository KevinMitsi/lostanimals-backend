package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.security.personal-data")
public class PersonalDataProtectionProperties {
    private String encryptionKey;
    private String lookupKey;
}
