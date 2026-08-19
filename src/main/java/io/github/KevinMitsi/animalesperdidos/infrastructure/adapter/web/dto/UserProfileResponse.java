package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import java.util.UUID;

public record UserProfileResponse(UUID userId, String email, String displayName, String phone,
                                  String documentNumber, String pictureUrl, boolean profileComplete) {
}
