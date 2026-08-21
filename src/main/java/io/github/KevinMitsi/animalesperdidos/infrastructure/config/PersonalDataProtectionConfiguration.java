package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.migration.EncryptExistingPersonalDataMigration;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security.PersonalDataCipher;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PersonalDataProtectionProperties.class)
public class PersonalDataProtectionConfiguration {
    @Bean
    PersonalDataCipher personalDataCipher(PersonalDataProtectionProperties properties) {
        return new PersonalDataCipher(properties.getEncryptionKey(), properties.getLookupKey());
    }

    @Bean
    FlywayConfigurationCustomizer personalDataMigration(PersonalDataCipher cipher) {
        return configuration -> configuration.javaMigrations(new EncryptExistingPersonalDataMigration(cipher));
    }
}
