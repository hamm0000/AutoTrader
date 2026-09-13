package dev.hamm0000.autotrader.adapter.postgres.order;

import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderQueryException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSaveException;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrderPersistenceAdapter implements SaveOrder, FindOrder {

    private final OrderRepository orders;

    OrderPersistenceAdapter(OrderRepository orders) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
    }

    @Override
    @Transactional
    public void save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        try {
            // flush 없이 커밋 시점까지 미루면 중복 키 실패가 이 메서드 밖에서 일어나 아래의 catch가 못 잡습니다
            orders.saveAndFlush(OrderEntityMapper.toEntity(order));
        } catch (DataAccessException exception) {
            throw new OrderSaveException("주문을 저장하지 못했습니다", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByIdempotencyKey(OrderIdempotencyKey idempotencyKey) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        try {
            return orders.findByIdempotencyKey(idempotencyKey.value())
                    .map(OrderEntityMapper::toDomain);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new OrderQueryException("주문을 조회하지 못했습니다", exception);
        }
    }
}
