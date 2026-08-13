package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel="spring")
public interface ContactWebMapper {
    ContactRequestUseCase.Command toCommand(CreateContactRequest request);
    ContactRequestResponse toResponse(ContactRequestUseCase.View value);
    List<ContactRequestResponse> toContactResponses(List<ContactRequestUseCase.View> values);
    ConversationResponse toResponse(ConversationUseCase.View value);
    ParticipantResponse toResponse(ConversationUseCase.ParticipantView value);
    List<ConversationResponse> toConversationResponses(List<ConversationUseCase.View> values);
    MessageResponse toResponse(ConversationUseCase.MessageView value);
    MessagePageResponse toResponse(ConversationUseCase.MessagePage value);
}
