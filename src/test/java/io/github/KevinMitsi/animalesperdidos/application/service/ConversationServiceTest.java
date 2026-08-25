package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    @Mock ContactRepository contacts; @Mock UserRepository users; @Mock MessageEventPublisher messageEvents;
    private static final Instant NOW=Instant.parse("2026-08-13T12:00:00Z");
    @Test void participantCanSendStoredMessage(){
        UUID actor=UUID.randomUUID(),other=UUID.randomUUID(); Conversation conversation=conversation(actor,other);
        when(contacts.findConversation(conversation.id())).thenReturn(done(Optional.of(conversation)));
        when(contacts.blockedBetween(actor,other)).thenReturn(done(false));
        when(contacts.saveMessage(any())).thenAnswer(invocation->done(invocation.getArgument(0)));
        when(messageEvents.publish(any())).thenReturn(done(null));
        UUID id=service().send(actor,conversation.id(),"Hola").toCompletableFuture().join();
        assertNotNull(id);
        verify(contacts).saveMessage(argThat(m->m.content().equals("Hola")));
        verify(messageEvents).publish(argThat(m->m.id().equals(id)&&m.content().equals("Hola")));
    }
    @Test void nonParticipantCannotDiscoverConversation(){
        Conversation conversation=conversation(UUID.randomUUID(),UUID.randomUUID());
        when(contacts.findConversation(conversation.id())).thenReturn(done(Optional.of(conversation)));
        CompletionException error=assertThrows(CompletionException.class,()->service()
                .send(UUID.randomUUID(),conversation.id(),"Hola").toCompletableFuture().join());
        assertInstanceOf(io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound.class,error.getCause());
    }
    @Test void pollingReturnsCheckpointEvenWithoutAnotherPage(){
        UUID actor=UUID.randomUUID(),other=UUID.randomUUID(); Conversation conversation=conversation(actor,other);
        Message message=new Message(UUID.randomUUID(),conversation.id(),other,"Información",NOW);
        when(contacts.findConversation(conversation.id())).thenReturn(done(Optional.of(conversation)));
        when(contacts.messages(conversation.id(),null,null,50)).thenReturn(done(List.of(message)));
        var page=service().messages(actor,conversation.id(),null,50).toCompletableFuture().join();
        assertEquals(1,page.items().size()); assertNotNull(page.nextAfter());
    }
    private ConversationService service(){return new ConversationService(
            contacts,users,messageEvents,Clock.fixed(NOW,ZoneOffset.UTC));}
    private static Conversation conversation(UUID a,UUID b){return Conversation.open(UUID.randomUUID(),UUID.randomUUID(),a,b,NOW);}
    private static <T> CompletableFuture<T> done(T value){return CompletableFuture.completedFuture(value);}
}
