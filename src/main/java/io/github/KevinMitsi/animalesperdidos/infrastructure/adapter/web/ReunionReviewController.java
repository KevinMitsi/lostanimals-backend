package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ReunionModerationUseCase;
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

@RestController @RequestMapping("/api/v1/lost-pet-reports") @RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth") @Tag(name="Reunion review requests")
public class ReunionReviewController {
    private final ReunionModerationUseCase moderation; private final AuthenticatedUserResolver authenticatedUser;
    private final CreationHttpResponseFactory responses;
    @PostMapping("/{reportId}/reunion-review") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Ask moderators to verify that the pet is physically reunited")
    public Mono<IdResponse> request(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID reportId,
            @Valid @RequestBody ReunionReviewRequest request){
        return Mono.fromCompletionStage(moderation.request(authenticatedUser.id(jwt),reportId,request.note())).map(responses::id);
    }
}
