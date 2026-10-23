package com.portfolio.platform.order;
import java.time.Instant;
import java.util.UUID;
public record OrderCreatedEvent(UUID eventId, UUID orderId, String customerId, String sku, int quantity, Instant occurredAt) {}

