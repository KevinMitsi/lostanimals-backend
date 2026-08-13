package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.ModerationWebMapper;
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
import java.util.*;

@RestController @RequestMapping("/api/v1/moderator") @RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth") @Tag(name="Moderator operations")
public class ModeratorController {
    private final ReunionModerationUseCase reunions; private final ContentModerationUseCase content;
    private final ModerationWebMapper mapper; private final AuthenticatedUserResolver authenticatedUser;
    @GetMapping("/reunion-reviews") @Operation(summary="List pending reunion verifications including the owner's phone")
    public Mono<List<ReunionReviewResponse>> reunionReviews(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(reunions.pending(authenticatedUser.id(jwt))).map(mapper::toReunionResponses);
    }
    @PatchMapping("/reunion-reviews/{reviewId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Approve or reject a reunion after human verification")
    public Mono<Void> decideReunion(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID reviewId,
            @Valid @RequestBody ModerationDecisionRequest request){
        return Mono.fromCompletionStage(reunions.decide(authenticatedUser.id(jwt),reviewId,request.approved(),request.note()));
    }
    @GetMapping("/conversation-reports") @Operation(summary="List pending conversation reports")
    public Mono<List<ConversationReportResponse>> conversationReports(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(content.pendingReports(authenticatedUser.id(jwt))).map(mapper::toReportResponses);
    }
    @PatchMapping("/conversation-reports/{reportId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Resolve or dismiss a conversation report")
    public Mono<Void> decideReport(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID reportId,
            @Valid @RequestBody ReportDecisionRequest request){
        return Mono.fromCompletionStage(content.decide(authenticatedUser.id(jwt),reportId,request.resolved()));
    }
}
