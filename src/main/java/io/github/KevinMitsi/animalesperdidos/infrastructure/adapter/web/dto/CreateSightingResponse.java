package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.Valid;
import java.util.UUID;

public record CreateSightingResponse(UUID id, boolean created,
                                     @Valid DuplicateSightingWarningResponse warning) { }
