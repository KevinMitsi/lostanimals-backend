package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record DepartmentResponse(@NotNull UUID id, @NotBlank @Size(max = 100) String name) { }
