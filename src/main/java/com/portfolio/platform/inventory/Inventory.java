package com.portfolio.platform.inventory;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="inventory") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Inventory {
    @Id @Column(length=64) private String sku;
    @Column(nullable=false) private int available;
    @Version private long version;
    public Inventory(String sku, int available) { this.sku=sku; this.available=available; }
    public void allocate(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        if (available < quantity) throw new InsufficientStockException(sku, available, quantity);
        available -= quantity;
    }
}

