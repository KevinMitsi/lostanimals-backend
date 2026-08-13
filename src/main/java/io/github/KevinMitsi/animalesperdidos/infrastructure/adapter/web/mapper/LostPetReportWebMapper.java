package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;
import io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus;

@Mapper(componentModel = "spring")
public interface LostPetReportWebMapper {
    @Mapping(target = "ownerId", source = "ownerId")
    ReportLostPetUseCase.Command toCommand(CreateLostPetReportRequest request, UUID ownerId);

    @Mapping(target = "ownerId", source = "ownerId")
    PrepareReportImageUploadUseCase.Command toCommand(PrepareImageUploadRequest request, UUID ownerId);

    ManageLostPetReportUseCase.Edit toCommand(EditLostPetReportRequest request);

    QueryLostPetReportsUseCase.Search toSearch(ReportSearchRequest request);

    ReportStatus toStatus(ReportStatusDto status);

    @Mapping(target = "id", source = "reportId")
    CreateLostPetReportResponse toResponse(ReportLostPetUseCase.Result result);

    PreparedImageUploadResponse toResponse(PrepareReportImageUploadUseCase.Result result);
    LostPetReportResponse toResponse(QueryLostPetReportsUseCase.ReportView result);
    LostPetImageResponse toResponse(QueryLostPetReportsUseCase.ImageView result);
    LostPetReportPageResponse toResponse(QueryLostPetReportsUseCase.Page result);
}
