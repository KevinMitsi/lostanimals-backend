package io.github.KevinMitsi.animalesperdidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class LostAnimals {

    public static void main(String[] args) {
        if (Boolean.parseBoolean(System.getenv("FLYWAY_ONLY"))) {
            new SpringApplicationBuilder(FlywayOnlyConfiguration.class)
                    .web(WebApplicationType.NONE)
                    .run(args);
            return;
        }

        SpringApplication.run(LostAnimals.class, args);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class FlywayOnlyConfiguration {
    }
}
