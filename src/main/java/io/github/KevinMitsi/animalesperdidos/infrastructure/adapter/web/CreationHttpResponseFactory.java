package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreationHttpResponseFactory {
    private final LostPetReportWebMapper lostPetMapper;
    private final SightingWebMapper sightingMapper;

    public ResponseEntity<CreateLostPetReportResponse> lostPet(ReportLostPetUseCase.Result result,
                                                                UriComponentsBuilder uriBuilder) {
        URI location = uriBuilder.path("/api/v1/lost-pet-reports/{id}").buildAndExpand(result.reportId()).toUri();
        return ResponseEntity.created(location).body(lostPetMapper.toResponse(result));
    }

    public ResponseEntity<CreateSightingResponse> sighting(CreateSightingUseCase.Result result,
                                                            UriComponentsBuilder uriBuilder) {
        CreateSightingResponse body = sightingMapper.toResponse(result);
        if (!result.created()) return ResponseEntity.ok(body);
        URI location = uriBuilder.path("/api/v1/sightings/{id}").buildAndExpand(result.sightingId()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    public AttachedImageResponse attachedImage(UUID imageId) { return new AttachedImageResponse(imageId); }
    public IdResponse id(UUID id) { return new IdResponse(id); }
}
