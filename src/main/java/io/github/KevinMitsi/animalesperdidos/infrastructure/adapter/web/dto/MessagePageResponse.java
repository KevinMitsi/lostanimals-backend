package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record MessagePageResponse(@NotNull List<@Valid MessageResponse> items,
                                  @Size(max=200) String nextAfter) { }
