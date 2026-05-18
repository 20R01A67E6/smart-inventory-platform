package com.inventory.ml.repository;

import com.inventory.ml.model.SalesHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, UUID> {

    @Query("SELECT s FROM SalesHistory s WHERE s.productId = :productId " +
           "AND s.saleDate >= :since ORDER BY s.saleDate DESC")
    List<SalesHistory> findByProductIdSince(UUID productId, LocalDate since);

    @Query("SELECT DISTINCT s.productId FROM SalesHistory s WHERE s.saleDate >= :since")
    List<UUID> findDistinctProductIdsSince(LocalDate since);

    @Query("SELECT s.productId, s.productName, SUM(s.quantitySold) " +
           "FROM SalesHistory s WHERE s.saleDate >= :since " +
           "GROUP BY s.productId, s.productName")
    List<Object[]> findTotalSalesByProductSince(LocalDate since);
}
