package com.portfolio.platform.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    @PostMapping public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        PurchaseOrder order=service.create(request.customerId(),request.sku(),request.quantity());
        return ResponseEntity.created(URI.create("/api/orders/"+order.getId())).body(OrderResponse.from(order));
    }
    @GetMapping("/{id}") public OrderResponse get(@PathVariable UUID id){return OrderResponse.from(service.get(id));}

    public record CreateOrderRequest(@NotBlank @Size(max=64) String customerId,@NotBlank @Size(max=64) String sku,@Min(1) @Max(1000) int quantity){}
    public record OrderResponse(UUID id,String customerId,String sku,int quantity,OrderStatus status,Instant createdAt){
        static OrderResponse from(PurchaseOrder o){return new OrderResponse(o.getId(),o.getCustomerId(),o.getSku(),o.getQuantity(),o.getStatus(),o.getCreatedAt());}
    }
}

