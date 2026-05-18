package com.inventory.inventory.repository;

import com.inventory.inventory.model.InventoryReservation;
import com.inventory.inventory.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    List<InventoryReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, UUID productId);

    List<InventoryReservation> findByOrderId(UUID orderId);
}
