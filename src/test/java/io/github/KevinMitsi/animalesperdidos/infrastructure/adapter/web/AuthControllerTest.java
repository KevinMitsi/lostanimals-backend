package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.AuthenticateUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.GoogleAuthenticationUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.PasswordRecoveryUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RefreshSessionUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.RegisterUserUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.in.VerifyEmailUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.AuthWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.mock;

class AuthControllerTest {

    @Test
    void exposesGoogleAuthenticationEndpoint() {
        AuthController controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(AuthenticateUserUseCase.class),
                mock(VerifyEmailUseCase.class),
                mock(PasswordRecoveryUseCase.class),
                mock(RefreshSessionUseCase.class),
                mock(GoogleAuthenticationUseCase.class),
                mock(AuthWebMapper.class),
                mock(ClientIpResolver.class));

        WebTestClient.bindToController(controller).build()
                .post()
                .uri("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
