package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SightingPageResponse(@NotNull List<@Valid SightingResponse> items,
                                   @Size(max = 200) String nextCursor) { }
