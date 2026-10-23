package com.portfolio.platform.shipping;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.UUID;
@Document("shipment_views")
public record ShipmentView(@Id UUID orderId,String customerId,String sku,int quantity,String state,Instant updatedAt) {}

