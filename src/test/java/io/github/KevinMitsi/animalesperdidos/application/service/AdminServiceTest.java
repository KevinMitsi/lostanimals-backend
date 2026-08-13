package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock ServiceAreaRepository areas; @Mock UserRepository users;
    @Test void administratorCannotRemoveOwnAdminRole(){
        UUID admin=UUID.randomUUID(); AdminService service=new AdminService(areas,users,Clock.systemUTC());
        when(users.findById(admin)).thenReturn(CompletableFuture.completedFuture(Optional.of(
                io.github.KevinMitsi.animalesperdidos.domain.model.User.register(admin,"admin@test.co","hash","3000000002",
                        "125","Admin",java.time.Instant.now()).changeRole(UserRole.ADMIN))));
        var error=assertThrows(java.util.concurrent.CompletionException.class,
                ()->service.changeRole(admin,admin,UserRole.USER).toCompletableFuture().join());
        assertInstanceOf(BusinessRuleViolation.class,error.getCause());
        verifyNoInteractions(areas);
    }
}
