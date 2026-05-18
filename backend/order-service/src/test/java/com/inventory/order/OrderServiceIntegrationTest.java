package com.inventory.order;

import com.inventory.common.dto.OrderItemRequest;
import com.inventory.common.dto.OrderRequest;
import com.inventory.order.model.Order;
import com.inventory.order.model.OrderStatus;
import com.inventory.order.repository.OutboxRepository;
import com.inventory.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void createOrder_shouldPersistOrderAndOutboxMessage() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderRequest request = new OrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 2)),
                "123 Main St, Springfield, IL 62701"
        );

        // Act
        Order order = orderService.createOrder(request);

        // Assert - order created with correct state
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);

        // Assert - outbox event was saved in same transaction
        var outboxMessages = outboxRepository.findAll();
        assertThat(outboxMessages).hasSize(1);
        assertThat(outboxMessages.get(0).getTopic()).isEqualTo("order.created");
        assertThat(outboxMessages.get(0).getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(outboxMessages.get(0).isProcessed()).isFalse();
        assertThat(outboxMessages.get(0).getPayload()).contains("orderId");
    }

    @Test
    void cancelOrder_shouldUpdateStatusAndReason() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Order order = orderService.createOrder(new OrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 1)),
                "456 Oak Ave, Portland, OR 97201"
        ));

        // Act
        orderService.cancelOrder(order.getId(), "Changed my mind");

        // Assert
        Order cancelled = orderService.getOrder(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Changed my mind");
    }

    @Test
    void confirmOrder_shouldSetStatusAndPaymentId() {
        // Arrange
        Order order = orderService.createOrder(new OrderRequest(
                UUID.randomUUID(),
                List.of(new OrderItemRequest(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"), 1)),
                "789 Pine Rd, Austin, TX 78701"
        ));
        UUID paymentId = UUID.randomUUID();

        // Act
        orderService.confirmOrder(order.getId(), paymentId);

        // Assert
        Order confirmed = orderService.getOrder(order.getId());
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.getPaymentId()).isEqualTo(paymentId);
    }
}
