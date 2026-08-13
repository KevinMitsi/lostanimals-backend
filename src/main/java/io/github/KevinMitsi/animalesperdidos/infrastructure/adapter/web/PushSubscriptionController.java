package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ManagePushSubscriptionUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/push-subscriptions") @RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth") @Tag(name="Push subscriptions")
public class PushSubscriptionController {
    private final ManagePushSubscriptionUseCase subscriptions; private final AuthenticatedUserResolver authenticatedUser;
    private final CreationHttpResponseFactory responses;
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Register a device with Amazon SNS")
    public Mono<IdResponse> register(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RegisterPushSubscriptionRequest request){
        return Mono.fromCompletionStage(subscriptions.register(authenticatedUser.id(jwt),request.deviceToken())).map(responses::id);
    }
    @DeleteMapping("/{subscriptionId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Remove an owned SNS endpoint")
    public Mono<Void> remove(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID subscriptionId){
        return Mono.fromCompletionStage(subscriptions.remove(authenticatedUser.id(jwt),subscriptionId));
    }
}
