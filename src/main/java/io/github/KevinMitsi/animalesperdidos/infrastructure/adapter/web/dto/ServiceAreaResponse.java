package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
public record ServiceAreaResponse(@NotBlank @Pattern(regexp = "^[0-9]{5}$") String municipalityCode,
                                  boolean enabled) { }
