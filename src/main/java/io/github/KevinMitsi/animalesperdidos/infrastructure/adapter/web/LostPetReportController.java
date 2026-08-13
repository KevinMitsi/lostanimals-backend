package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.LostPetReportWebMapper;
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
@RequestMapping("/api/v1/lost-pet-reports")
@RequiredArgsConstructor
@Tag(name = "Lost pet reports")
public class LostPetReportController {
    private final ReportLostPetUseCase createReport;
    private final PrepareReportImageUploadUseCase prepareUpload;
    private final QueryLostPetReportsUseCase queries;
    private final ManageLostPetReportUseCase management;
    private final LostPetReportWebMapper mapper;

    @PostMapping("/image-uploads")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a short-lived direct S3 upload URL")
    public Mono<PreparedImageUploadResponse> prepareUpload(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody PrepareImageUploadRequest request) {
        return Mono.fromCompletionStage(prepareUpload.prepare(mapper.toCommand(request, owner(jwt))))
                .map(mapper::toResponse);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish a report using previously uploaded image keys",
            responses = {@ApiResponse(responseCode = "201", description = "Report published")})
    public Mono<ResponseEntity<CreateLostPetReportResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateLostPetReportRequest request, UriComponentsBuilder uriBuilder) {
        return Mono.fromCompletionStage(createReport.report(mapper.toCommand(request, owner(jwt))))
                .map(result -> {
                    URI location = uriBuilder.path("/api/v1/lost-pet-reports/{id}")
                            .buildAndExpand(result.reportId()).toUri();
                    return ResponseEntity.created(location).body(mapper.toResponse(result));
                });
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get a public report; coordinates are deliberately approximated")
    public Mono<LostPetReportResponse> get(@PathVariable UUID reportId) {
        return Mono.fromCompletionStage(queries.getPublic(reportId)).map(mapper::toResponse);
    }

    @GetMapping
    @Operation(summary = "Search public reports with cursor pagination")
    public Mono<LostPetReportPageResponse> search(@Valid @ModelAttribute ReportSearchRequest request) {
        return Mono.fromCompletionStage(queries.searchPublic(toSearch(request))).map(mapper::toResponse);
    }

    @GetMapping("/mine")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List the authenticated user's reports with exact coordinates")
    public Mono<LostPetReportPageResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @ModelAttribute ReportSearchRequest request) {
        return Mono.fromCompletionStage(queries.mine(owner(jwt), toSearch(request))).map(mapper::toResponse);
    }

    @PutMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit an active report owned by the authenticated user")
    public Mono<Void> edit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                           @Valid @RequestBody EditLostPetReportRequest request) {
        return Mono.fromCompletionStage(management.edit(owner(jwt), reportId, mapper.toCommand(request)));
    }

    @PatchMapping("/{reportId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a report as reunited/closed, or reopen it within 30 days")
    public Mono<Void> close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                            @Valid @RequestBody CloseLostPetReportRequest request) {
        return Mono.fromCompletionStage(management.close(owner(jwt), reportId,
                ReportStatus.valueOf(request.status().name())));
    }

    @PostMapping("/{reportId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Attach a directly uploaded image to an owned report")
    public Mono<AttachedImageResponse> addImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                                @Valid @RequestBody AttachImageRequest request) {
        return Mono.fromCompletionStage(management.addImage(owner(jwt), reportId, request.objectKey()))
                .map(AttachedImageResponse::new);
    }

    @DeleteMapping("/{reportId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove an image while retaining at least one")
    public Mono<Void> removeImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                  @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.removeImage(owner(jwt), reportId, imageId));
    }

    @PutMapping("/{reportId}/images/{imageId}/primary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Select the primary report image")
    public Mono<Void> setPrimary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                 @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.setPrimaryImage(owner(jwt), reportId, imageId));
    }

    private QueryLostPetReportsUseCase.Search toSearch(ReportSearchRequest request) {
        ReportCursorDecoder.Cursor cursor = ReportCursorDecoder.decode(request.cursor());
        return new QueryLostPetReportsUseCase.Search(
                request.species() == null ? null : Species.valueOf(request.species().name()),
                request.neighborhoodId(),
                request.status() == null ? null : ReportStatus.valueOf(request.status().name()),
                cursor.createdAt(), cursor.id(), request.limit());
    }

    private static UUID owner(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
