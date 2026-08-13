package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;
import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import java.util.List;
@Mapper(componentModel="spring")
public interface ModerationWebMapper {
    ReunionReviewResponse toResponse(ReunionModerationUseCase.View value);
    List<ReunionReviewResponse> toReunionResponses(List<ReunionModerationUseCase.View> values);
    ConversationReportResponse toResponse(ContentModerationUseCase.View value);
    List<ConversationReportResponse> toReportResponses(List<ContentModerationUseCase.View> values);
}
