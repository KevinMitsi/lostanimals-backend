package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.CompleteGoogleProfileUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CompleteGoogleProfileRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User profile")
public class UserProfileController {
    private final CompleteGoogleProfileUseCase completeProfile;
    private final AuthenticatedUserResolver authenticatedUser;

    @PutMapping("/profile")
    @Operation(summary = "Complete the phone and document fields missing after Google registration")
    public Mono<UserProfileResponse> complete(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody CompleteGoogleProfileRequest request) {
        CompleteGoogleProfileUseCase.Command command = new CompleteGoogleProfileUseCase.Command(
                authenticatedUser.id(jwt), request.phone(), request.documentNumber());
        return Mono.fromCompletionStage(completeProfile.complete(command)).map(result -> new UserProfileResponse(
                result.userId(), result.email(), result.displayName(), result.phone(), result.documentNumber(),
                result.pictureUrl(), result.profileComplete()));
    }
}
