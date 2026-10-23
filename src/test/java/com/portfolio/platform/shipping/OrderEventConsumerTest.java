package com.portfolio.platform.shipping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.platform.order.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import java.time.*;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderEventConsumerTest {
    @Test void duplicateEventIsIgnored() throws Exception {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        ValueOperations<String,String> values=mock(ValueOperations.class);
        ShipmentViewRepository repo=mock(ShipmentViewRepository.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(),eq("1"),any(Duration.class))).thenReturn(false);
        OrderCreatedEvent event=new OrderCreatedEvent(UUID.randomUUID(),UUID.randomUUID(),"c","SKU-001",1,Instant.now());
        new OrderEventConsumer(new ObjectMapper().findAndRegisterModules(),redis,repo).onOrderCreated(new ObjectMapper().findAndRegisterModules().writeValueAsString(event));
        verifyNoInteractions(repo);
    }
}

