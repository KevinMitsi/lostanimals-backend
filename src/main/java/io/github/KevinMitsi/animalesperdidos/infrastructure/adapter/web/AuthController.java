package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.VerifyEmailUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PasswordRecoveryUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RefreshSessionUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.GoogleAuthenticationUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.LoginRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisterUserRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.RegisteredUserResponse;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
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
    private final VerifyEmailUseCase verifyEmail;
    private final PasswordRecoveryUseCase passwordRecovery;
    private final RefreshSessionUseCase refreshSessions;
    private final GoogleAuthenticationUseCase googleAuthentication;
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

    @PostMapping("/google")
    @Operation(summary = "Register or sign in with a Google Identity Services credential", responses = {
            @ApiResponse(responseCode = "200", description = "Application session issued"),
            @ApiResponse(responseCode = "401", description = "Invalid Google credential"),
            @ApiResponse(responseCode = "422", description = "Consent is missing for a new account")
    })
    public Mono<GoogleAuthenticationResponse> google(@Valid @RequestBody GoogleAuthenticationRequest request) {
        return Mono.fromCompletionStage(googleAuthentication.authenticate(
                        new GoogleAuthenticationUseCase.Command(request.credential(), request.acceptsDataProcessing())))
                .map(mapper::toResponse);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify an email with a one-time token",
            responses = {@ApiResponse(responseCode = "204", description = "Email verified"),
                    @ApiResponse(responseCode = "422", description = "Token invalid, expired or used")})
    public Mono<Void> verifyEmail(@Valid @RequestBody OpaqueTokenRequest request) {
        return Mono.fromCompletionStage(verifyEmail.verify(request.token()));
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request another verification email",
            description = "Always returns 202 to prevent account enumeration")
    public Mono<Void> resendVerification(@Valid @RequestBody EmailActionRequest request,
                                         ServerHttpRequest httpRequest) {
        return Mono.fromCompletionStage(verifyEmail.resend(request.email(), request.turnstileToken(),
                clientIpResolver.resolve(httpRequest)));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request a password reset",
            description = "Always returns 202 to prevent account enumeration")
    public Mono<Void> forgotPassword(@Valid @RequestBody EmailActionRequest request,
                                     ServerHttpRequest httpRequest) {
        return Mono.fromCompletionStage(passwordRecovery.request(request.email(), request.turnstileToken(),
                clientIpResolver.resolve(httpRequest)));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password using a one-time reset token")
    public Mono<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return Mono.fromCompletionStage(passwordRecovery.reset(request.token(), request.newPassword()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token and issue a new token pair")
    public Mono<TokenResponse> refresh(@Valid @RequestBody OpaqueTokenRequest request) {
        return Mono.fromCompletionStage(refreshSessions.refresh(request.token())).map(mapper::toResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a refresh token")
    public Mono<Void> logout(@Valid @RequestBody OpaqueTokenRequest request) {
        return Mono.fromCompletionStage(refreshSessions.logout(request.token()));
    }
}
