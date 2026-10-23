package com.portfolio.platform.shipping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/shipments") @RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentViewRepository repository;
    @GetMapping("/{orderId}") ShipmentView get(@PathVariable UUID orderId){return repository.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));}
}

