package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.port.out.PushSubscriptionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ManagePushSubscriptionServiceTest {
    @Mock PushSubscriptionPort subscriptions;
    @Test void delegatesDeviceRegistrationBehindPort(){UUID user=UUID.randomUUID(),id=UUID.randomUUID();
        when(subscriptions.register(user,"device-token")).thenReturn(CompletableFuture.completedFuture(id));
        assertEquals(id,new ManagePushSubscriptionService(subscriptions).register(user,"device-token").toCompletableFuture().join());}
}
