package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record ContactRequestResponse(@NotNull UUID id, @NotNull PublicationTypeDto publicationType,
        @NotNull UUID publicationId, @NotNull UUID requesterId, @NotNull UUID recipientId,
        @NotNull ContactRequestStatusDto status, @NotBlank @Size(max=500) String note,
        @NotNull Instant createdAt, Instant answeredAt) { }
