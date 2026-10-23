package com.portfolio.platform.outbox;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="outbox_events") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id private UUID id;
    @Column(nullable=false, length=80) private String aggregateType;
    @Column(nullable=false, length=80) private String eventType;
    @Column(nullable=false, length=100) private String aggregateId;
    @Lob @Column(nullable=false, columnDefinition="TEXT") private String payload;
    @Column(nullable=false) private Instant createdAt;
    private Instant publishedAt;
    public OutboxEvent(UUID id,String aggregateType,String eventType,String aggregateId,String payload) {
        this.id=id; this.aggregateType=aggregateType; this.eventType=eventType; this.aggregateId=aggregateId;
        this.payload=payload; this.createdAt=Instant.now();
    }
    public void markPublished() { this.publishedAt=Instant.now(); }
}

