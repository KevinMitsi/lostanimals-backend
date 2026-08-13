package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RefreshSessionUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.LoginRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisterUserRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisteredUserResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.TokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthWebMapper {
    @Mapping(target = "remoteIp", source = "remoteIp")
    RegisterUserUseCase.Command toCommand(RegisterUserRequest request, String remoteIp);

    @Mapping(target = "remoteIp", source = "remoteIp")
    AuthenticateUserUseCase.Command toCommand(LoginRequest request, String remoteIp);

    RegisteredUserResponse toResponse(RegisterUserUseCase.Result result);
    TokenResponse toResponse(AuthenticateUserUseCase.Result result);
    TokenResponse toResponse(RefreshSessionUseCase.Result result);
}
