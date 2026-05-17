package com.inventory.ml.repository;

import com.inventory.ml.model.RestockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestockAlertRepository extends JpaRepository<RestockAlert, UUID> {

    List<RestockAlert> findByProductIdOrderByGeneratedAtDesc(UUID productId);

    List<RestockAlert> findByAcknowledgedFalseOrderByDaysUntilStockoutAsc();

    Optional<RestockAlert> findTopByProductIdOrderByGeneratedAtDesc(UUID productId);
}
