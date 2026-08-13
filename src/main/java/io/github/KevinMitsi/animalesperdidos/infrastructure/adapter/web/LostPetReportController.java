package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
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
    private final AuthenticatedUserResolver authenticatedUser;
    private final CreationHttpResponseFactory responses;

    @PostMapping("/image-uploads")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a short-lived direct S3 upload URL")
    public Mono<PreparedImageUploadResponse> prepareUpload(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody PrepareImageUploadRequest request) {
        return Mono.fromCompletionStage(prepareUpload.prepare(mapper.toCommand(request, authenticatedUser.id(jwt))))
                .map(mapper::toResponse);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish a report using previously uploaded image keys",
            responses = {@ApiResponse(responseCode = "201", description = "Report published")})
    public Mono<ResponseEntity<CreateLostPetReportResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateLostPetReportRequest request, UriComponentsBuilder uriBuilder) {
        return Mono.fromCompletionStage(createReport.report(mapper.toCommand(request, authenticatedUser.id(jwt))))
                .map(result -> responses.lostPet(result, uriBuilder));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get a public report; coordinates are deliberately approximated")
    public Mono<LostPetReportResponse> get(@PathVariable UUID reportId) {
        return Mono.fromCompletionStage(queries.getPublic(reportId)).map(mapper::toResponse);
    }

    @GetMapping
    @Operation(summary = "Search public reports with cursor pagination")
    public Mono<LostPetReportPageResponse> search(@Valid @ModelAttribute ReportSearchRequest request) {
        return Mono.fromCompletionStage(queries.searchPublic(mapper.toSearch(request))).map(mapper::toResponse);
    }

    @GetMapping("/mine")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List the authenticated user's reports with exact coordinates")
    public Mono<LostPetReportPageResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @ModelAttribute ReportSearchRequest request) {
        return Mono.fromCompletionStage(queries.mine(authenticatedUser.id(jwt), mapper.toSearch(request))).map(mapper::toResponse);
    }

    @PutMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit an active report owned by the authenticated user")
    public Mono<Void> edit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                           @Valid @RequestBody EditLostPetReportRequest request) {
        return Mono.fromCompletionStage(management.edit(authenticatedUser.id(jwt), reportId, mapper.toCommand(request)));
    }

    @PatchMapping("/{reportId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a report as reunited/closed, or reopen it within 30 days")
    public Mono<Void> close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                            @Valid @RequestBody CloseLostPetReportRequest request) {
        return Mono.fromCompletionStage(management.close(authenticatedUser.id(jwt), reportId,
                mapper.toStatus(request.status())));
    }

    @PostMapping("/{reportId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Attach a directly uploaded image to an owned report")
    public Mono<AttachedImageResponse> addImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                                @Valid @RequestBody AttachImageRequest request) {
        return Mono.fromCompletionStage(management.addImage(authenticatedUser.id(jwt), reportId, request.objectKey()))
                .map(responses::attachedImage);
    }

    @DeleteMapping("/{reportId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove an image while retaining at least one")
    public Mono<Void> removeImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                  @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.removeImage(authenticatedUser.id(jwt), reportId, imageId));
    }

    @PutMapping("/{reportId}/images/{imageId}/primary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Select the primary report image")
    public Mono<Void> setPrimary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId,
                                 @PathVariable UUID imageId) {
        return Mono.fromCompletionStage(management.setPrimaryImage(authenticatedUser.id(jwt), reportId, imageId));
    }

}
