package dev.hamm0000.autotrader.adapter.postgres.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);
}
