package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
public record ReunionReviewRequest(@NotBlank @Size(max=500) String note) { }
