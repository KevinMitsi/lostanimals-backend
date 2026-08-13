package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CreateLostPetReportRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CreateLostPetReportResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.LostPetReportWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lost-pet-reports")
@RequiredArgsConstructor
@Tag(name = "Lost pet reports")
@SecurityRequirement(name = "bearerAuth")
public class LostPetReportController {
    private final ReportLostPetUseCase useCase;
    private final LostPetReportWebMapper mapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publish a lost-pet report", responses = {
            @ApiResponse(responseCode = "201", description = "Report published"),
            @ApiResponse(responseCode = "401", description = "JWT missing or invalid"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    public Mono<ResponseEntity<CreateLostPetReportResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestPart("metadata") CreateLostPetReportRequest metadata,
            @RequestPart("images") Flux<FilePart> files,
            UriComponentsBuilder uriBuilder) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return files.concatMap(this::readImage)
                .collectList()
                .map(images -> mapper.toCommand(metadata, ownerId, images))
                .flatMap(command -> Mono.fromCompletionStage(useCase.report(command)))
                .map(result -> {
                    URI location = uriBuilder.path("/api/v1/lost-pet-reports/{id}")
                            .buildAndExpand(result.reportId()).toUri();
                    return ResponseEntity.created(location).body(mapper.toResponse(result));
                });
    }

    private Mono<ReportLostPetUseCase.Image> readImage(FilePart file) {
        return DataBufferUtils.join(file.content()).map(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            MediaType mediaType = file.headers().getContentType();
            String contentType = mediaType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mediaType.toString();
            return new ReportLostPetUseCase.Image(file.filename(), contentType, bytes);
        });
    }
}
