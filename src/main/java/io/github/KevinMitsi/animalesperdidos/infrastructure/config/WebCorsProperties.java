package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("app.web.cors")
public class WebCorsProperties {
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
