package com.portfolio.platform.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.platform.inventory.*;
import com.portfolio.platform.outbox.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    PurchaseOrderRepository orders=mock(PurchaseOrderRepository.class);
    InventoryRepository inventory=mock(InventoryRepository.class);
    OutboxEventRepository outbox=mock(OutboxEventRepository.class);
    OrderService service=new OrderService(orders,inventory,outbox,new ObjectMapper().findAndRegisterModules());

    @Test void allocatesInventoryAndWritesOutboxAtomically() {
        Inventory stock=new Inventory("SKU-001",10);
        when(inventory.findForUpdate("SKU-001")).thenReturn(Optional.of(stock));
        when(orders.save(any())).thenAnswer(i->i.getArgument(0));
        service.create("customer-1","SKU-001",3);
        assertThat(stock.getAvailable()).isEqualTo(7);
        verify(outbox).save(argThat(e->e.getEventType().equals("OrderCreated") && e.getPayload().contains("customer-1")));
    }

    @Test void rejectsInsufficientStockWithoutCreatingOrder() {
        when(inventory.findForUpdate("SKU-001")).thenReturn(Optional.of(new Inventory("SKU-001",1)));
        assertThatThrownBy(()->service.create("customer-1","SKU-001",2)).isInstanceOf(InsufficientStockException.class);
        verifyNoInteractions(orders,outbox);
    }
}

