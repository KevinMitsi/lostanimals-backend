package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
@Validated
@SecurityRequirement(name="bearerAuth") @Tag(name="Administrator operations")
public class AdminController {
    private final AdminUseCase administration; private final ReunionModerationUseCase reunions;
    private final AdminWebMapper adminMapper; private final ModerationWebMapper moderationMapper;
    private final AuthenticatedUserResolver authenticatedUser;
    @GetMapping("/service-areas") @Operation(summary="List persisted municipality availability configurations")
    public Mono<List<ServiceAreaResponse>> serviceAreas(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(administration.serviceAreas(authenticatedUser.id(jwt))).map(adminMapper::toResponses);
    }
    @PutMapping("/service-areas/{municipalityCode}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Enable or disable a municipality; municipalities are enabled by default")
    public Mono<Void> setServiceArea(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @jakarta.validation.constraints.Pattern(regexp="^[0-9]{5}$") String municipalityCode,
            @Valid @RequestBody SetServiceAreaRequest request){
        return Mono.fromCompletionStage(administration.setServiceArea(authenticatedUser.id(jwt),municipalityCode,request.enabled()));
    }
    @PutMapping("/users/{userId}/role") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Assign USER, MODERATOR or ADMIN role")
    public Mono<Void> changeRole(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request){
        return Mono.fromCompletionStage(administration.changeRole(authenticatedUser.id(jwt),userId,adminMapper.toRole(request.role())));
    }
    @GetMapping("/reunion-reviews") @Operation(summary="List pending reunion verifications")
    public Mono<List<ReunionReviewResponse>> reunionReviews(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(reunions.pending(authenticatedUser.id(jwt))).map(moderationMapper::toReunionResponses);
    }
    @PatchMapping("/reunion-reviews/{reviewId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Approve or reject a reunion as administrator")
    public Mono<Void> decideReunion(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID reviewId,
            @Valid @RequestBody ModerationDecisionRequest request){
        return Mono.fromCompletionStage(reunions.decide(authenticatedUser.id(jwt),reviewId,request.approved(),request.note()));
    }
}
