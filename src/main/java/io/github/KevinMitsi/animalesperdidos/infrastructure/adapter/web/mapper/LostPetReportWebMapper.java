package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;
import io.github.KevinMitsi.animalesperdidos.domain.model.ReportStatus;
import io.github.KevinMitsi.animalesperdidos.domain.model.AdministrativeLocation;

@Mapper(componentModel = "spring")
public interface LostPetReportWebMapper {
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "administrativeLocation", expression = "java(location(request.departmentCode(), request.municipalityCode(), request.neighborhood()))")
    ReportLostPetUseCase.Command toCommand(CreateLostPetReportRequest request, UUID ownerId);

    @Mapping(target = "ownerId", source = "ownerId")
    PrepareReportImageUploadUseCase.Command toCommand(PrepareImageUploadRequest request, UUID ownerId);

    @Mapping(target = "administrativeLocation", expression = "java(location(request.departmentCode(), request.municipalityCode(), request.neighborhood()))")
    ManageLostPetReportUseCase.Edit toCommand(EditLostPetReportRequest request);

    default AdministrativeLocation location(String departmentCode, String municipalityCode, String neighborhood) {
        return new AdministrativeLocation(departmentCode, municipalityCode, neighborhood);
    }

    QueryLostPetReportsUseCase.Search toSearch(ReportSearchRequest request);

    ReportStatus toStatus(ReportStatusDto status);
    ReportStatus toStatus(OwnerReportStatusDto status);

    @Mapping(target = "id", source = "reportId")
    CreateLostPetReportResponse toResponse(ReportLostPetUseCase.Result result);

    PreparedImageUploadResponse toResponse(PrepareReportImageUploadUseCase.Result result);
    LostPetReportResponse toResponse(QueryLostPetReportsUseCase.ReportView result);
    LostPetImageResponse toResponse(QueryLostPetReportsUseCase.ImageView result);
    LostPetReportPageResponse toResponse(QueryLostPetReportsUseCase.Page result);
}
