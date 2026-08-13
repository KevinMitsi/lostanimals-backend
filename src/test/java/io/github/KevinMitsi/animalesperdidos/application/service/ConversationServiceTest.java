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
    @Mock ContactRepository contacts; @Mock UserRepository users;
    private static final Instant NOW=Instant.parse("2026-08-13T12:00:00Z");
    @Test void participantCanSendStoredMessage(){
        UUID actor=UUID.randomUUID(),other=UUID.randomUUID(); Conversation conversation=conversation(actor,other);
        when(contacts.findConversation(conversation.id())).thenReturn(done(Optional.of(conversation)));
        when(contacts.blockedBetween(actor,other)).thenReturn(done(false));
        when(contacts.saveMessage(any())).thenAnswer(invocation->done(invocation.getArgument(0)));
        UUID id=new ConversationService(contacts,users,Clock.fixed(NOW,ZoneOffset.UTC)).send(actor,conversation.id(),"Hola")
                .toCompletableFuture().join(); assertNotNull(id); verify(contacts).saveMessage(argThat(m->m.content().equals("Hola")));
    }
    @Test void nonParticipantCannotDiscoverConversation(){
        Conversation conversation=conversation(UUID.randomUUID(),UUID.randomUUID());
        when(contacts.findConversation(conversation.id())).thenReturn(done(Optional.of(conversation)));
        CompletionException error=assertThrows(CompletionException.class,()->new ConversationService(contacts,users,
                Clock.fixed(NOW,ZoneOffset.UTC)).send(UUID.randomUUID(),conversation.id(),"Hola").toCompletableFuture().join());
        assertInstanceOf(io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound.class,error.getCause());
    }
    private static Conversation conversation(UUID a,UUID b){return Conversation.open(UUID.randomUUID(),UUID.randomUUID(),a,b,NOW);}
    private static <T> CompletableFuture<T> done(T value){return CompletableFuture.completedFuture(value);}
}
