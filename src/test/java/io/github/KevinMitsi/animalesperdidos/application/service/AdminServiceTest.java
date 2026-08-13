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

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock ServiceAreaRepository areas; @Mock UserRepository users;
    @Test void administratorCannotRemoveOwnAdminRole(){
        UUID admin=UUID.randomUUID(); AdminService service=new AdminService(areas,users,Clock.systemUTC());
        assertThrows(BusinessRuleViolation.class,()->service.changeRole(admin,admin,UserRole.USER));
        verifyNoInteractions(users,areas);
    }
}
