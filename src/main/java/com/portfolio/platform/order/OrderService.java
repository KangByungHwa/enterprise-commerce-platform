package com.portfolio.platform.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.platform.inventory.*;
import com.portfolio.platform.outbox.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class OrderService {
    private final PurchaseOrderRepository orders;
    private final InventoryRepository inventory;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    @Transactional
    public PurchaseOrder create(String customerId, String sku, int quantity) {
        Inventory stock=inventory.findForUpdate(sku).orElseThrow(() -> new IllegalArgumentException("Unknown sku: "+sku));
        stock.allocate(quantity);
        PurchaseOrder order=orders.save(new PurchaseOrder(UUID.randomUUID(), customerId, sku, quantity));
        UUID eventId=UUID.randomUUID();
        OrderCreatedEvent event=new OrderCreatedEvent(eventId,order.getId(),customerId,sku,quantity,Instant.now());
        try {
            outbox.save(new OutboxEvent(eventId,"Order","OrderCreated",order.getId().toString(),objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize order event",e); }
        return order;
    }

    @Transactional(readOnly=true)
    public PurchaseOrder get(UUID id) { return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id)); }
}

