package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import jakarta.validation.constraints.NotNull;
public record ChangeRoleRequest(@NotNull UserRoleDto role) { }
