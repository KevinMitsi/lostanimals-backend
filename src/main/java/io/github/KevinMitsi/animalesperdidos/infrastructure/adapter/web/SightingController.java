package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.SightingWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sightings")
@RequiredArgsConstructor
@Tag(name = "Sightings", description = "Animal sightings reported by the community")
public class SightingController {
    private final CreateSightingUseCase creation;
    private final PrepareSightingImageUploadUseCase uploads;
    private final QuerySightingsUseCase queries;
    private final ManageSightingUseCase management;
    private final SightingWebMapper mapper;

    @PostMapping("/image-uploads")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a short-lived direct S3 upload URL for a sighting")
    public Mono<PreparedImageUploadResponse> prepareUpload(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PrepareImageUploadRequest request) {
        return Mono.fromCompletionStage(uploads.prepare(mapper.toCommand(request, actor(jwt)))).map(mapper::toResponse);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish a sighting", description = "If a sighting of the same species exists within 50 m and two hours, returns a warning without publishing until explicitly confirmed.",
            responses = {@ApiResponse(responseCode = "201", description = "Sighting published"),
                    @ApiResponse(responseCode = "200", description = "Possible duplicate; confirmation required")})
    public Mono<ResponseEntity<CreateSightingResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSightingRequest request, UriComponentsBuilder uriBuilder) {
        return Mono.fromCompletionStage(creation.create(mapper.toCommand(request, actor(jwt)))).map(result -> {
            CreateSightingResponse body = mapper.toResponse(result);
            if (!result.created()) return ResponseEntity.ok(body);
            URI location = uriBuilder.path("/api/v1/sightings/{id}").buildAndExpand(result.sightingId()).toUri();
            return ResponseEntity.created(location).body(body);
        });
    }

    @GetMapping("/{sightingId}")
    @Operation(summary = "Get a public sighting with deliberately approximated coordinates")
    public Mono<SightingResponse> get(@PathVariable UUID sightingId) {
        return Mono.fromCompletionStage(queries.getPublic(sightingId)).map(mapper::toResponse);
    }

    @GetMapping
    @Operation(summary = "Search public sightings with cursor pagination")
    public Mono<SightingPageResponse> search(@Valid @ModelAttribute SightingSearchRequest request) {
        return Mono.fromCompletionStage(queries.searchPublic(toSearch(request))).map(mapper::toResponse);
    }

    @GetMapping("/mine")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List the authenticated user's sightings with exact coordinates")
    public Mono<SightingPageResponse> mine(@AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute SightingSearchRequest request) {
        return Mono.fromCompletionStage(queries.mine(actor(jwt), toSearch(request))).map(mapper::toResponse);
    }

    @PutMapping("/{sightingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit an active sighting owned by the authenticated user")
    public Mono<Void> edit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sightingId,
            @Valid @RequestBody EditSightingRequest request) {
        return Mono.fromCompletionStage(management.edit(actor(jwt), sightingId, mapper.toCommand(request)));
    }

    @PatchMapping("/{sightingId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Close an owned sighting")
    public Mono<Void> close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sightingId) {
        return Mono.fromCompletionStage(management.close(actor(jwt), sightingId));
    }

    @PostMapping("/{sightingId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Attach a directly uploaded image to an owned sighting")
    public Mono<AttachedImageResponse> addImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sightingId,
            @Valid @RequestBody AttachImageRequest request) {
        return Mono.fromCompletionStage(management.addImage(actor(jwt), sightingId, request.objectKey()))
                .map(AttachedImageResponse::new);
    }

    @DeleteMapping("/{sightingId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove an image while retaining at least one")
    public Mono<Void> removeImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sightingId,
            @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.removeImage(actor(jwt), sightingId, imageId));
    }

    @PutMapping("/{sightingId}/images/{imageId}/primary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Select the primary sighting image")
    public Mono<Void> setPrimary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sightingId,
            @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.setPrimary(actor(jwt), sightingId, imageId));
    }

    private static QuerySightingsUseCase.Search toSearch(SightingSearchRequest request) {
        ReportCursorDecoder.Cursor cursor = ReportCursorDecoder.decode(request.cursor());
        return new QuerySightingsUseCase.Search(
                request.species() == null ? null : Species.valueOf(request.species().name()),
                request.neighborhoodId(),
                request.status() == null ? null : SightingStatus.valueOf(request.status().name()),
                cursor.createdAt(), cursor.id(), request.limit());
    }

    private static UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
