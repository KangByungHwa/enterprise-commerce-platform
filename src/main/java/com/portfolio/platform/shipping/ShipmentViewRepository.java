package com.portfolio.platform.shipping;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.UUID;
public interface ShipmentViewRepository extends MongoRepository<ShipmentView,UUID> {}

