package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;
@NoSqlInjection
public record ReunionReviewRequest(@NotBlank @Size(max=500) String note) { }
