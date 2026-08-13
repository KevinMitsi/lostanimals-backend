package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.ReportLostPetUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CreateLostPetReportRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CreateLostPetReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface LostPetReportWebMapper {
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "images", source = "images")
    ReportLostPetUseCase.Command toCommand(CreateLostPetReportRequest request, UUID ownerId,
                                            List<ReportLostPetUseCase.Image> images);

    @Mapping(target = "id", source = "reportId")
    CreateLostPetReportResponse toResponse(ReportLostPetUseCase.Result result);
}
