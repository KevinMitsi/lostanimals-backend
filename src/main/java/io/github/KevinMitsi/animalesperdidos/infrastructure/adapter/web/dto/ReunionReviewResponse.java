package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record ReunionReviewResponse(@NotNull UUID id, @NotNull UUID reportId, @NotNull UUID ownerId,
        @NotBlank String ownerName, @NotBlank String ownerPhone, @NotBlank @Size(max=500) String requestNote,
        @NotNull ReunionReviewStatusDto status, @NotNull Instant createdAt) { }
