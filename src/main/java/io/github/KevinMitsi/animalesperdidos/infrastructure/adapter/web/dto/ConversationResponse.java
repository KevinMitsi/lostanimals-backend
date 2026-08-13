package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public record ConversationResponse(@NotNull UUID id, @NotNull ConversationStatusDto status,
        @NotEmpty @Size(min=2,max=2) List<@Valid ParticipantResponse> participants,
        @NotNull Instant createdAt, Instant closedAt) { }
