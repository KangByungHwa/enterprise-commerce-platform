package com.portfolio.platform.order;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="purchase_orders") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PurchaseOrder {
    @Id private UUID id;
    @Column(nullable=false, length=64) private String customerId;
    @Column(nullable=false, length=64) private String sku;
    @Column(nullable=false) private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private OrderStatus status;
    @Column(nullable=false, updatable=false) private Instant createdAt;

    public PurchaseOrder(UUID id, String customerId, String sku, int quantity) {
        this.id=id; this.customerId=customerId; this.sku=sku; this.quantity=quantity;
        this.status=OrderStatus.ALLOCATED; this.createdAt=Instant.now();
    }
}

