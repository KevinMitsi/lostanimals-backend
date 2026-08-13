package io.github.KevinMitsi.animalesperdidos.application.service;
import io.github.KevinMitsi.animalesperdidos.application.port.out.*;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReunionModerationServiceTest {
    @Mock ModerationRepository moderation; @Mock LostPetReportRepository reports; @Mock UserRepository users;
    private static final Instant NOW=Instant.parse("2026-08-13T12:00:00Z");
    @Test void moderatorApprovalAtomicallyPassesReunitedReportToRepository(){
        LostPetReport report=ManageLostPetReportServiceTest.report(List.of("key")); UUID moderator=UUID.randomUUID();
        ReunionReview review=ReunionReview.request(UUID.randomUUID(),report.id(),report.ownerId(),"Ya está conmigo",NOW.minusSeconds(60));
        when(moderation.findReview(review.id())).thenReturn(done(Optional.of(review)));
        when(reports.findById(report.id())).thenReturn(done(Optional.of(report)));
        when(moderation.decide(any(),any())).thenAnswer(invocation->done(invocation.getArgument(0)));
        new ReunionModerationService(moderation,reports,users,Clock.fixed(NOW,ZoneOffset.UTC))
                .decide(moderator,review.id(),true,"Verificado por llamada").toCompletableFuture().join();
        verify(moderation).decide(argThat(r->r.status()==ReunionReview.Status.APPROVED),
                argThat(r->r.status()==ReportStatus.REUNITED));
    }
    private static <T> CompletableFuture<T> done(T value){return CompletableFuture.completedFuture(value);}
}
