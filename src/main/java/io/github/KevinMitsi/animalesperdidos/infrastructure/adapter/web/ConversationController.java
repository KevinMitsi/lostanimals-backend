package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ConversationUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.ContactWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.*;

@RestController @RequestMapping("/api/v1/conversations") @RequiredArgsConstructor @Validated
@SecurityRequirement(name="bearerAuth") @Tag(name="Conversations")
public class ConversationController {
    private final ConversationUseCase conversations; private final ContactWebMapper mapper;
    private final AuthenticatedUserResolver authenticatedUser; private final CreationHttpResponseFactory responses;
    @GetMapping @Operation(summary="List internal conversations without contact details")
    public Mono<List<ConversationResponse>> list(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(conversations.list(authenticatedUser.id(jwt))).map(mapper::toConversationResponses);
    }
    @GetMapping("/{conversationId}/messages")
    @Operation(summary="Poll stored messages after an opaque cursor")
    public Mono<MessagePageResponse> messages(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID conversationId,
            @RequestParam(required=false) @Size(max=200) String after,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){
        return Mono.fromCompletionStage(conversations.messages(authenticatedUser.id(jwt),conversationId,after,limit)).map(mapper::toResponse);
    }
    @PostMapping("/{conversationId}/messages") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Store a message in an open conversation")
    public Mono<IdResponse> send(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request){
        return Mono.fromCompletionStage(conversations.send(authenticatedUser.id(jwt),conversationId,request.content())).map(responses::id);
    }
    @PatchMapping("/{conversationId}/close") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Close a conversation")
    public Mono<Void> close(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID conversationId){
        return Mono.fromCompletionStage(conversations.close(authenticatedUser.id(jwt),conversationId));
    }
    @PatchMapping("/{conversationId}/block") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Block the other participant and close the conversation")
    public Mono<Void> block(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID conversationId){
        return Mono.fromCompletionStage(conversations.block(authenticatedUser.id(jwt),conversationId));
    }
    @PostMapping("/{conversationId}/reports") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Report a conversation for moderator review")
    public Mono<IdResponse> report(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID conversationId,
            @Valid @RequestBody ReportConversationRequest request){
        return Mono.fromCompletionStage(conversations.report(authenticatedUser.id(jwt),conversationId,
                request.reason(),request.details())).map(responses::id);
    }
}
