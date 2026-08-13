package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.LoginRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisterUserRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisteredUserResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.TokenResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.AuthWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final AuthWebMapper mapper;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a citizen account", responses = {
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "409", description = "Email, phone or document already registered"),
            @ApiResponse(responseCode = "422", description = "Bot or business validation failed")
    })
    public Mono<RegisteredUserResponse> register(@Valid @RequestBody RegisterUserRequest request,
                                                  ServerHttpRequest httpRequest) {
        return Mono.fromCompletionStage(registerUser.register(
                        mapper.toCommand(request, clientIpResolver.resolve(httpRequest))))
                .map(mapper::toResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain a JWT", responses = {
            @ApiResponse(responseCode = "200", description = "JWT issued"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "422", description = "Bot validation failed")
    })
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request, ServerHttpRequest httpRequest) {
        return Mono.fromCompletionStage(authenticateUser.authenticate(
                        mapper.toCommand(request, clientIpResolver.resolve(httpRequest))))
                .map(mapper::toResponse);
    }
}
