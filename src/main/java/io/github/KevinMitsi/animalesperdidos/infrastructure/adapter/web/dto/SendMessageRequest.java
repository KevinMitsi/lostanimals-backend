package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
public record SendMessageRequest(@NotBlank @Size(max=2000) String content) { }
