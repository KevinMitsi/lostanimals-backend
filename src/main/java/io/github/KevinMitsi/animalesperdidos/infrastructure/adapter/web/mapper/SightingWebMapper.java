package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.*;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SightingWebMapper {
    @Mapping(target = "reporterId", source = "reporterId")
    CreateSightingUseCase.Command toCommand(CreateSightingRequest request, UUID reporterId);

    @Mapping(target = "reporterId", source = "reporterId")
    PrepareSightingImageUploadUseCase.Command toCommand(PrepareImageUploadRequest request, UUID reporterId);

    ManageSightingUseCase.Edit toCommand(EditSightingRequest request);

    @Mapping(target = "id", source = "sightingId")
    CreateSightingResponse toResponse(CreateSightingUseCase.Result result);
    DuplicateSightingWarningResponse toResponse(CreateSightingUseCase.DuplicateWarning warning);
    PreparedImageUploadResponse toResponse(PrepareSightingImageUploadUseCase.Result result);
    SightingResponse toResponse(QuerySightingsUseCase.View result);
    SightingImageResponse toResponse(QuerySightingsUseCase.ImageView result);
    SightingPageResponse toResponse(QuerySightingsUseCase.Page result);
}
