package com.portfolio.platform.inventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface InventoryRepository extends JpaRepository<Inventory,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.sku = :sku") Optional<Inventory> findForUpdate(@Param("sku") String sku);
}

