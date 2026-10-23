package com.portfolio.platform.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.platform.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

@Component @RequiredArgsConstructor
public class OrderEventConsumer {
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final ShipmentViewRepository shipments;

    @KafkaListener(topics="${app.kafka.order-topic:order.created.v1}",groupId="shipping-projection")
    public void onOrderCreated(String json) throws Exception {
        OrderCreatedEvent event=mapper.readValue(json,OrderCreatedEvent.class);
        String key="event:processed:"+event.eventId();
        Boolean first=redis.opsForValue().setIfAbsent(key,"1",Duration.ofDays(7));
        if (!Boolean.TRUE.equals(first)) return;
        try {
            shipments.save(new ShipmentView(event.orderId(),event.customerId(),event.sku(),event.quantity(),"READY_TO_SHIP",event.occurredAt()));
        } catch (RuntimeException e) { redis.delete(key); throw e; }
    }
}

