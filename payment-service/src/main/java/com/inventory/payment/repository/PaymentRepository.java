package com.inventory.payment.repository;

import com.inventory.payment.model.Payment;
import com.inventory.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Payment> findByStatus(PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
