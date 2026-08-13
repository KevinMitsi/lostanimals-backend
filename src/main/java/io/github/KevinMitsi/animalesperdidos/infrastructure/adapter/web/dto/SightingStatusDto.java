package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a sighting")
public enum SightingStatusDto { ACTIVE, CLOSED }
