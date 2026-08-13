package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QueryLostPetReportsUseCase;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.application.port.out.LostPetReportRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.LostPetReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.github.KevinMitsi.animalesperdidos.application.service.AuthenticationServicesTest.completed;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryLostPetReportsServiceTest {
    @Mock LostPetReportRepository repository;
    @Mock ImageStoragePort storage;
    @Captor ArgumentCaptor<LostPetReportRepository.SearchCriteria> criteriaCaptor;

    @Test
    void publicDetailApproximatesCoordinatesAndSignsImages() {
        LostPetReport report = ManageLostPetReportServiceTest.report(List.of("key-1"));
        when(repository.findById(report.id())).thenReturn(completed(Optional.of(report)));
        when(storage.createDownloadUrl("key-1", Duration.ofMinutes(15)))
                .thenReturn(completed("https://s3/read"));

        QueryLostPetReportsUseCase.ReportView result = new QueryLostPetReportsService(repository, storage)
                .getPublic(report.id()).toCompletableFuture().join();

        assertEquals(4.534, result.latitude());
        assertEquals(-75.681, result.longitude());
        assertEquals("https://s3/read", result.images().getFirst().url());
    }

    @Test
    void mineUsesExactCoordinatesAndRequestsOneExtraRowForCursor() {
        LostPetReport first = ManageLostPetReportServiceTest.report(List.of("key-1"));
        LostPetReport second = ManageLostPetReportServiceTest.report(List.of("key-2"));
        when(repository.search(any())).thenReturn(completed(List.of(first, second)));
        when(storage.createDownloadUrl(anyString(), eq(Duration.ofMinutes(15))))
                .thenAnswer(invocation -> completed("https://s3/" + invocation.getArgument(0)));
        QueryLostPetReportsUseCase.Search search = new QueryLostPetReportsUseCase.Search(
                null, null, null, null, null, 1);

        QueryLostPetReportsUseCase.Page page = new QueryLostPetReportsService(repository, storage)
                .mine(first.ownerId(), search).toCompletableFuture().join();

        verify(repository).search(criteriaCaptor.capture());
        assertEquals(2, criteriaCaptor.getValue().limit());
        assertEquals(first.lastSeenAt().latitude(), page.items().getFirst().latitude());
        assertNotNull(page.nextCursor());
        assertEquals(1, page.items().size());
    }
}
