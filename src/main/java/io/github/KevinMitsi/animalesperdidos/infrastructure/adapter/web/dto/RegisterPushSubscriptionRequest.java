package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
@NoSqlInjection
public record RegisterPushSubscriptionRequest(@NotBlank @Size(max=4096)
        @Schema(description="APNs/FCM device token issued to the client application") String deviceToken) { }
