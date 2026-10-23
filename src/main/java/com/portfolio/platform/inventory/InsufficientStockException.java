package com.portfolio.platform.inventory;
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock: sku="+sku+", available="+available+", requested="+requested);
    }
}

