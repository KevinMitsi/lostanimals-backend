package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ContactRequestUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.ContactWebMapper;
import io.swagger.v3.oas.annotations.*;
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

@RestController @RequestMapping("/api/v1/contact-requests") @RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth") @Tag(name="Contact requests")
public class ContactRequestController {
    private final ContactRequestUseCase contacts; private final ContactWebMapper mapper;
    private final AuthenticatedUserResolver authenticatedUser; private final CreationHttpResponseFactory responses;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Request internal contact without revealing personal data")
    public Mono<IdResponse> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CreateContactRequest request){
        return Mono.fromCompletionStage(contacts.create(authenticatedUser.id(jwt),mapper.toCommand(request))).map(responses::id);
    }
    @GetMapping("/received") @Operation(summary="List contact requests received by the authenticated publisher")
    public Mono<List<ContactRequestResponse>> received(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(contacts.received(authenticatedUser.id(jwt))).map(mapper::toContactResponses);
    }
    @GetMapping("/sent") @Operation(summary="List contact requests sent by the authenticated user")
    public Mono<List<ContactRequestResponse>> sent(@AuthenticationPrincipal Jwt jwt){
        return Mono.fromCompletionStage(contacts.sent(authenticatedUser.id(jwt))).map(mapper::toContactResponses);
    }
    @PatchMapping("/{requestId}/accept") @Operation(summary="Accept a request and atomically open an internal conversation")
    public Mono<IdResponse> accept(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){
        return Mono.fromCompletionStage(contacts.accept(authenticatedUser.id(jwt),requestId)).map(responses::id);
    }
    @PatchMapping("/{requestId}/reject") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Reject a received contact request")
    public Mono<Void> reject(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){
        return Mono.fromCompletionStage(contacts.reject(authenticatedUser.id(jwt),requestId));
    }
    @PatchMapping("/{requestId}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Cancel a sent contact request")
    public Mono<Void> cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){
        return Mono.fromCompletionStage(contacts.cancel(authenticatedUser.id(jwt),requestId));
    }
}
