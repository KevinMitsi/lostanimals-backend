package io.github.KevinMitsi.animalesperdidos.domain.model;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ContactDomainTest {
    private static final Instant NOW=Instant.parse("2026-08-13T12:00:00Z");
    @Test void recipientAcceptsAndConversationKeepsTwoParticipants(){
        UUID requester=UUID.randomUUID(),recipient=UUID.randomUUID();
        ContactRequest request=ContactRequest.create(UUID.randomUUID(),new PublicationRef(PublicationType.SIGHTING,UUID.randomUUID()),
                requester,recipient,"Lo vi cerca",NOW);
        assertEquals(ContactRequest.Status.ACCEPTED,request.accept(recipient,NOW.plusSeconds(1)).status());
        Conversation conversation=Conversation.open(UUID.randomUUID(),request.id(),requester,recipient,NOW);
        assertEquals(2,conversation.participants().size()); assertTrue(conversation.hasParticipant(requester));
    }
    @Test void requesterCannotAcceptTheirOwnRequest(){
        UUID requester=UUID.randomUUID();
        ContactRequest request=ContactRequest.create(UUID.randomUUID(),new PublicationRef(PublicationType.SIGHTING,UUID.randomUUID()),
                requester,UUID.randomUUID(),"Nota",NOW);
        assertThrows(IllegalStateException.class,()->request.accept(requester,NOW));
    }
    @Test void messageContentIsValidatedByDomain(){
        assertThrows(IllegalArgumentException.class,()->new Message(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID()," ",NOW));
    }
}
