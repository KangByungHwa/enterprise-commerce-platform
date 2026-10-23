package com.portfolio.platform.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String,String> kafka;
    @Value("${app.kafka.order-topic:order.created.v1}") private String topic;

    @Scheduled(fixedDelayString="${app.outbox.interval-ms:1000}")
    @Transactional
    public void publishBatch() {
        repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc().forEach(event -> {
            try {
                kafka.send(topic,event.getAggregateId(),event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) { throw new IllegalStateException("Kafka publication failed; event remains pending",e); }
        });
    }
}

