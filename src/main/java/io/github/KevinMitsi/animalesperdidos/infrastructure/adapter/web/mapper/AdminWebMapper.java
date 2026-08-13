package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;
import io.github.KevinMitsi.animalesperdidos.application.port.in.AdminUseCase;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import java.util.List;
@Mapper(componentModel="spring")
public interface AdminWebMapper {
    ServiceAreaResponse toResponse(AdminUseCase.ServiceAreaView value);
    List<ServiceAreaResponse> toResponses(List<AdminUseCase.ServiceAreaView> values);
    UserRole toRole(UserRoleDto role);
}
