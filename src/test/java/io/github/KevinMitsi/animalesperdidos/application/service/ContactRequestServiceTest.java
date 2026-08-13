package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.port.in.ContactRequestUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceTest {
    @Mock ContactRepository contacts; @Mock LostPetReportRepository reports; @Mock SightingRepository sightings;
    private ContactRequestService service; private static final Instant NOW=Instant.parse("2026-08-13T12:00:00Z");
    @BeforeEach void setup(){service=new ContactRequestService(contacts,reports,sightings,Clock.fixed(NOW,ZoneOffset.UTC));}
    @Test void createsRequestWithoutReadingPrivateUserData(){
        Sighting sighting=sighting(); UUID requester=UUID.randomUUID();
        when(sightings.findById(sighting.id())).thenReturn(done(Optional.of(sighting)));
        when(contacts.blockedBetween(requester,sighting.reporterId())).thenReturn(done(false));
        when(contacts.saveRequest(any())).thenAnswer(invocation->done(invocation.getArgument(0)));
        UUID id=service.create(requester,new ContactRequestUseCase.Command(PublicationType.SIGHTING,sighting.id(),"Tengo información"))
                .toCompletableFuture().join();
        assertNotNull(id); verify(contacts).saveRequest(argThat(r->r.recipientId().equals(sighting.reporterId())));
        verifyNoInteractions(reports);
    }
    @Test void blockPreventsCreatingContact(){
        Sighting sighting=sighting(); UUID requester=UUID.randomUUID();
        when(sightings.findById(sighting.id())).thenReturn(done(Optional.of(sighting)));
        when(contacts.blockedBetween(requester,sighting.reporterId())).thenReturn(done(true));
        CompletionException error=assertThrows(CompletionException.class,()->service.create(requester,
                new ContactRequestUseCase.Command(PublicationType.SIGHTING,sighting.id(),"Nota")).toCompletableFuture().join());
        assertInstanceOf(ForbiddenOperation.class,error.getCause()); verify(contacts,never()).saveRequest(any());
    }
    private static Sighting sighting(){return Sighting.create(UUID.randomUUID(),UUID.randomUUID(),Species.DOG,"Descripción",
            NOW.minusSeconds(30),new GeoPoint(4.53,-75.68),UUID.randomUUID(),List.of("key"),NOW);}
    private static <T> CompletableFuture<T> done(T value){return CompletableFuture.completedFuture(value);}
}
