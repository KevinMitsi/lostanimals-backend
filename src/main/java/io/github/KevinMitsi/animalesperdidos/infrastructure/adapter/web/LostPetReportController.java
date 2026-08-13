package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.domain.model.Species;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lost-pet-reports")
@RequiredArgsConstructor
public class LostPetReportController {
    private final ReportLostPetUseCase useCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CreateReportResponse>> create(
            @RequestHeader("X-Owner-Id") UUID ownerId,
            @Valid @RequestPart("metadata") CreateReportMetadata metadata,
            @RequestPart("images") Flux<FilePart> files,
            UriComponentsBuilder uriBuilder) {
        return files.concatMap(this::readImage)
                .collectList()
                .map(images -> metadata.toCommand(ownerId, images))
                .flatMap(command -> Mono.fromCompletionStage(useCase.report(command)))
                .map(result -> {
                    URI location = uriBuilder.path("/api/v1/lost-pet-reports/{id}")
                            .buildAndExpand(result.reportId()).toUri();
                    return ResponseEntity.created(location).body(new CreateReportResponse(result.reportId()));
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

    public record CreateReportMetadata(
            @NotBlank @Size(max = 80) String petName,
            @NotNull Species species,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @PastOrPresent Instant disappearedAt,
            double latitude,
            double longitude,
            @NotNull UUID neighborhoodId
    ) {
        ReportLostPetUseCase.Command toCommand(UUID ownerId, List<ReportLostPetUseCase.Image> images) {
            return new ReportLostPetUseCase.Command(ownerId, petName, species, description, disappearedAt,
                    latitude, longitude, neighborhoodId, images);
        }
    }

    public record CreateReportResponse(UUID id) {
    }
}
