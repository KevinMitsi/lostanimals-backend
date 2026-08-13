package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("app.cloudflare.turnstile")
public class CloudflareProperties {
    private boolean enabled;
    private String secretKey;
    private String expectedHostname;
    private boolean trustConnectingIp;

}
