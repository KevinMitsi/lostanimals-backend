package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ModerationRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository @RequiredArgsConstructor
public class R2dbcModerationRepository implements ModerationRepository {
    private final DatabaseClient databaseClient; private final TransactionalOperator transaction;
    @Override public CompletionStage<ReunionReview> save(ReunionReview r) {
        return databaseClient.sql("""
                INSERT INTO reunion_review(id,report_id,requested_by,request_note,status,created_at)
                VALUES(:id,:report,:requester,:note,:status,:created)
                """).bind("id",r.id()).bind("report",r.reportId()).bind("requester",r.requestedBy())
                .bind("note",r.requestNote()).bind("status",r.status().name()).bind("created",r.createdAt())
                .fetch().rowsUpdated().thenReturn(r).onErrorMap(DataIntegrityViolationException.class,
                        ignored -> new BusinessRuleViolation("A reunion review is already pending"))
                .toFuture();
    }
    @Override public CompletionStage<Optional<ReunionReview>> findReview(UUID id) {
        return databaseClient.sql("SELECT * FROM reunion_review WHERE id=:id").bind("id",id).map(this::row).one()
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }
    @Override public CompletionStage<List<ReunionReview>> pendingReviews(int limit) {
        return databaseClient.sql("SELECT * FROM reunion_review WHERE status='PENDING' ORDER BY created_at,id LIMIT :limit")
                .bind("limit",limit).map(this::row).all().collectList().toFuture();
    }
    @Override public CompletionStage<ReunionReview> decide(ReunionReview review, LostPetReport report) {
        Mono<Long> reviewUpdate=databaseClient.sql("""
                UPDATE reunion_review SET status=:status,reviewed_by=:reviewer,review_note=:note,reviewed_at=:reviewed
                WHERE id=:id AND status='PENDING'
                """).bind("status",review.status().name()).bind("reviewer",review.reviewedBy())
                .bind("note",review.reviewNote()).bind("reviewed",review.reviewedAt()).bind("id",review.id())
                .fetch().rowsUpdated().flatMap(rows->rows==1?Mono.just(rows):Mono.error(new ConcurrentUpdate()));
        Mono<Long> reportUpdate=review.status()==ReunionReview.Status.APPROVED?databaseClient.sql("""
                UPDATE lost_pet_report SET status='REUNITED',updated_at=:updated,version=version+1
                WHERE id=:id AND status='LOST' AND version=:version
                """).bind("updated",report.updatedAt()).bind("id",report.id()).bind("version",report.version())
                .fetch().rowsUpdated().flatMap(rows->rows==1?Mono.just(rows):Mono.error(new ConcurrentUpdate())):Mono.just(0L);
        return transaction.transactional(reviewUpdate.then(reportUpdate).thenReturn(review)).toFuture();
    }
    @Override public CompletionStage<Optional<ConversationReport>> findConversationReport(UUID id) {
        return databaseClient.sql("SELECT * FROM conversation_report WHERE id=:id").bind("id",id)
                .map(this::conversationReport).one().map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }
    @Override public CompletionStage<List<ConversationReport>> pendingConversationReports(int limit) {
        return databaseClient.sql("SELECT * FROM conversation_report WHERE status='PENDING' ORDER BY created_at,id LIMIT :limit")
                .bind("limit",limit).map(this::conversationReport).all().collectList().toFuture();
    }
    @Override public CompletionStage<ConversationReport> decideConversationReport(ConversationReport report) {
        return databaseClient.sql("""
                UPDATE conversation_report SET status=:status,reviewed_by=:reviewer,reviewed_at=:reviewed
                WHERE id=:id AND status='PENDING'
                """).bind("status",report.status().name()).bind("reviewer",report.reviewedBy())
                .bind("reviewed",report.reviewedAt()).bind("id",report.id()).fetch().rowsUpdated()
                .flatMap(rows->rows==1?Mono.just(report):Mono.error(new ConcurrentUpdate())).toFuture();
    }
    private ReunionReview row(io.r2dbc.spi.Row row,io.r2dbc.spi.RowMetadata metadata) {
        return new ReunionReview(row.get("id",UUID.class),row.get("report_id",UUID.class),row.get("requested_by",UUID.class),
                row.get("request_note",String.class),ReunionReview.Status.valueOf(row.get("status",String.class)),
                row.get("created_at",Instant.class),row.get("reviewed_by",UUID.class),row.get("review_note",String.class),
                row.get("reviewed_at",Instant.class));
    }
    private ConversationReport conversationReport(io.r2dbc.spi.Row row,io.r2dbc.spi.RowMetadata metadata) {
        return new ConversationReport(row.get("id",UUID.class),row.get("conversation_id",UUID.class),
                row.get("reporter_id",UUID.class),row.get("reason",String.class),row.get("details",String.class),
                ConversationReport.Status.valueOf(row.get("status",String.class)),row.get("created_at",Instant.class),
                row.get("reviewed_by",UUID.class),row.get("reviewed_at",Instant.class));
    }
}
