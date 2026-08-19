package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class GoogleAuthConfiguration {

    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(GoogleAuthProperties properties)
            throws GeneralSecurityException, IOException {
        return new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(properties.getClientId()))
                .build();
    }
}
