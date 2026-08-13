package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence;

import io.github.KevinMitsi.animalesperdidos.application.exception.*;
import io.github.KevinMitsi.animalesperdidos.application.port.out.ContactRepository;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Repository @RequiredArgsConstructor
public class R2dbcContactRepository implements ContactRepository {
    private final DatabaseClient databaseClient; private final TransactionalOperator transaction;
    @Override public CompletionStage<ContactRequest> saveRequest(ContactRequest r) {
        return databaseClient.sql("""
                INSERT INTO contact_request(id,publication_type,publication_id,requester_id,recipient_id,status,note,created_at,version)
                VALUES(:id,:type,:publication,:requester,:recipient,:status,:note,:created,:version)
                """).bind("id",r.id()).bind("type",r.publication().type().name()).bind("publication",r.publication().id())
                .bind("requester",r.requesterId()).bind("recipient",r.recipientId()).bind("status",r.status().name())
                .bind("note",r.note()).bind("created",r.createdAt()).bind("version",r.version()).fetch().rowsUpdated()
                .thenReturn(r).onErrorMap(DataIntegrityViolationException.class,
                        ignored -> new BusinessRuleViolation("A pending contact request already exists")).toFuture();
    }
    @Override public CompletionStage<Optional<ContactRequest>> findRequest(UUID id) {
        return databaseClient.sql("SELECT * FROM contact_request WHERE id=:id").bind("id",id).map(this::request).one()
                .map(Optional::of).defaultIfEmpty(Optional.empty()).toFuture();
    }
    @Override public CompletionStage<ContactRequest> updateRequest(ContactRequest r) { return updateRequestMono(r).toFuture(); }
    @Override public CompletionStage<Conversation> accept(ContactRequest request, Conversation conversation) {
        Mono<Long> insertConversation=databaseClient.sql("""
                INSERT INTO conversation(id,contact_request_id,status,created_at,version)
                VALUES(:id,:request,:status,:created,:version)
                """).bind("id",conversation.id()).bind("request",conversation.contactRequestId())
                .bind("status",conversation.status().name()).bind("created",conversation.createdAt())
                .bind("version",conversation.version()).fetch().rowsUpdated();
        Flux<Long> participants=Flux.fromIterable(conversation.participants()).concatMap(p -> databaseClient.sql("""
                INSERT INTO conversation_participant(conversation_id,user_id,joined_at) VALUES(:conversation,:user,:joined)
                """).bind("conversation",conversation.id()).bind("user",p.userId()).bind("joined",p.joinedAt()).fetch().rowsUpdated());
        return transaction.transactional(updateRequestMono(request).then(insertConversation).thenMany(participants)
                .then(Mono.just(conversation))).toFuture();
    }
    @Override public CompletionStage<List<ContactRequest>> requestsFor(UUID userId, boolean received) {
        String field=received?"recipient_id":"requester_id";
        return databaseClient.sql("SELECT * FROM contact_request WHERE "+field+"=:user ORDER BY created_at DESC,id DESC LIMIT 100")
                .bind("user",userId).map(this::request).all().collectList().toFuture();
    }
    @Override public CompletionStage<Optional<Conversation>> findConversation(UUID id) {
        return conversations("WHERE c.id=:id",id).map(values -> values.stream().findFirst()).toFuture();
    }
    @Override public CompletionStage<List<Conversation>> conversationsFor(UUID userId) {
        return conversations("WHERE EXISTS(SELECT 1 FROM conversation_participant mine WHERE mine.conversation_id=c.id AND mine.user_id=:id)",userId).toFuture();
    }
    @Override public CompletionStage<Conversation> updateConversation(Conversation c) { return updateConversationMono(c).toFuture(); }
    @Override public CompletionStage<Message> saveMessage(Message m) {
        return databaseClient.sql("""
                INSERT INTO conversation_message(id,conversation_id,sender_id,content,created_at)
                VALUES(:id,:conversation,:sender,:content,:created)
                """).bind("id",m.id()).bind("conversation",m.conversationId()).bind("sender",m.senderId())
                .bind("content",m.content()).bind("created",m.createdAt()).fetch().rowsUpdated().thenReturn(m).toFuture();
    }
    @Override public CompletionStage<List<Message>> messages(UUID conversationId, Instant after, UUID afterId, int limit) {
        String cursor=after!=null&&afterId!=null?" AND (created_at,id)>(:after,:afterId)":"";
        var spec=databaseClient.sql("SELECT * FROM conversation_message WHERE conversation_id=:conversation"+cursor+
                " ORDER BY created_at,id LIMIT :limit").bind("conversation",conversationId).bind("limit",limit);
        if(after!=null&&afterId!=null)spec=spec.bind("after",after).bind("afterId",afterId);
        return spec.map((row,metadata)->new Message(row.get("id",UUID.class),row.get("conversation_id",UUID.class),
                row.get("sender_id",UUID.class),row.get("content",String.class),row.get("created_at",Instant.class)))
                .all().collectList().toFuture();
    }
    @Override public CompletionStage<Boolean> blockedBetween(UUID first, UUID second) {
        return databaseClient.sql("""
                SELECT EXISTS(SELECT 1 FROM user_block WHERE
                  (blocker_id=:first AND blocked_id=:second) OR (blocker_id=:second AND blocked_id=:first)) blocked
                """).bind("first",first).bind("second",second)
                .map((row,metadata)->Boolean.TRUE.equals(row.get("blocked",Boolean.class))).one().defaultIfEmpty(false).toFuture();
    }
    @Override public CompletionStage<Void> saveBlock(UserBlock block, Conversation closed) {
        Mono<Long> insert=databaseClient.sql("""
                INSERT INTO user_block(blocker_id,blocked_id,conversation_id,created_at)
                VALUES(:blocker,:blocked,:conversation,:created) ON CONFLICT(blocker_id,blocked_id) DO NOTHING
                """).bind("blocker",block.blockerId()).bind("blocked",block.blockedId())
                .bind("conversation",block.conversationId()).bind("created",block.createdAt()).fetch().rowsUpdated();
        Mono<?> update=closed.status()==Conversation.Status.CLOSED&&closed.version()>=0
                ? updateConversationMono(closed) : Mono.just(closed);
        return transaction.transactional(insert.then(update).then()).toFuture();
    }
    @Override public CompletionStage<ConversationReport> saveReport(ConversationReport r) {
        return databaseClient.sql("""
                INSERT INTO conversation_report(id,conversation_id,reporter_id,reason,details,status,created_at)
                VALUES(:id,:conversation,:reporter,:reason,:details,:status,:created)
                """).bind("id",r.id()).bind("conversation",r.conversationId()).bind("reporter",r.reporterId())
                .bind("reason",r.reason()).bind("details",r.details()).bind("status",r.status().name())
                .bind("created",r.createdAt()).fetch().rowsUpdated().thenReturn(r)
                .onErrorMap(DataIntegrityViolationException.class, ignored -> new BusinessRuleViolation("Conversation already reported"))
                .toFuture();
    }
    private Mono<ContactRequest> updateRequestMono(ContactRequest r) {
        return databaseClient.sql("""
                UPDATE contact_request SET status=:status,answered_at=:answered,version=version+1
                WHERE id=:id AND version=:version
                """).bind("status",r.status().name())
                .bind("answered",r.answeredAt()==null?io.r2dbc.spi.Parameters.in(Instant.class):r.answeredAt())
                .bind("id",r.id()).bind("version",r.version()).fetch().rowsUpdated()
                .flatMap(rows->rows==1?Mono.just(r):Mono.error(new ConcurrentUpdate()));
    }
    private Mono<Conversation> updateConversationMono(Conversation c) {
        return databaseClient.sql("""
                UPDATE conversation SET status=:status,closed_at=:closed,version=version+1 WHERE id=:id AND version=:version
                """).bind("status",c.status().name())
                .bind("closed",c.closedAt()==null?io.r2dbc.spi.Parameters.in(Instant.class):c.closedAt())
                .bind("id",c.id()).bind("version",c.version()).fetch().rowsUpdated()
                .flatMap(rows->rows==1?Mono.just(c):Mono.error(new ConcurrentUpdate()));
    }
    private ContactRequest request(io.r2dbc.spi.Row row,io.r2dbc.spi.RowMetadata metadata) {
        return new ContactRequest(row.get("id",UUID.class),new PublicationRef(PublicationType.valueOf(row.get("publication_type",String.class)),
                row.get("publication_id",UUID.class)),row.get("requester_id",UUID.class),row.get("recipient_id",UUID.class),
                ContactRequest.Status.valueOf(row.get("status",String.class)),row.get("note",String.class),
                row.get("created_at",Instant.class),row.get("answered_at",Instant.class),row.get("version",Long.class));
    }
    private Mono<List<Conversation>> conversations(String where,UUID id) {
        return databaseClient.sql("""
                SELECT c.id,c.contact_request_id,c.status,c.created_at,c.closed_at,c.version,
                  p.user_id,p.joined_at,p.left_at FROM conversation c
                  JOIN conversation_participant p ON p.conversation_id=c.id %s
                  ORDER BY c.created_at DESC,c.id,p.user_id
                """.formatted(where)).bind("id",id).map((row,metadata)->new ConversationRow(row.get("id",UUID.class),
                row.get("contact_request_id",UUID.class),Conversation.Status.valueOf(row.get("status",String.class)),
                row.get("created_at",Instant.class),row.get("closed_at",Instant.class),row.get("version",Long.class),
                new Participant(row.get("user_id",UUID.class),row.get("joined_at",Instant.class),row.get("left_at",Instant.class))))
                .all().collectList().map(rows->{Map<UUID,List<ConversationRow>> grouped=new LinkedHashMap<>();
                    rows.forEach(r->grouped.computeIfAbsent(r.id(),ignored->new ArrayList<>()).add(r));
                    return grouped.values().stream().map(group->{var f=group.getFirst();return new Conversation(f.id(),f.requestId(),
                            f.status(),group.stream().map(ConversationRow::participant).toList(),f.created(),f.closed(),f.version());}).toList();});
    }
    private record ConversationRow(UUID id,UUID requestId,Conversation.Status status,Instant created,
                                   Instant closed,long version,Participant participant){}
}
