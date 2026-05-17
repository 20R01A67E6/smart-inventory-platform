package com.inventory.order.repository;

import com.inventory.order.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query("SELECT o FROM OutboxMessage o WHERE o.processed = false AND o.retryCount < 5 ORDER BY o.createdAt ASC")
    List<OutboxMessage> findPendingMessages();

    @Query("SELECT COUNT(o) FROM OutboxMessage o WHERE o.processed = false")
    long countPendingMessages();
}
