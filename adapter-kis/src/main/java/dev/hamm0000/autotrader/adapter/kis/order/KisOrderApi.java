package dev.hamm0000.autotrader.adapter.kis.order;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.application.port.out.SubmitOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.order.domain.OrderState;
import dev.hamm0000.autotrader.trading.order.domain.OrderType;
import java.util.Objects;
import org.springframework.web.client.RestClient;

public final class KisOrderApi implements SubmitOrder {

    private static final String ORDER_PATH = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String PAPER_BUY_TRANSACTION_ID = "VTTC0012U";
    private static final String PAPER_SELL_TRANSACTION_ID = "VTTC0011U";

    private final KisOrderHttpClient httpClient;
    private final String accountNumber;
    private final String accountProductCode;

    public KisOrderApi(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret,
            String accountNumber,
            String accountProductCode
    ) {
        this.httpClient = new KisOrderHttpClient(
                restClient,
                tokenProvider,
                appKey,
                appSecret
        );
        this.accountNumber = requireNotBlank(accountNumber, "accountNumber");
        this.accountProductCode = requireNotBlank(accountProductCode, "accountProductCode");
    }

    @Override
    public OrderAcknowledgment submit(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        requirePendingMarketOrder(order);

        KisOrderRequest request = KisOrderRequest.market(
                order,
                accountNumber,
                accountProductCode
        );
        return httpClient.post(
                ORDER_PATH,
                transactionId(order.side()),
                request
        ).toAcknowledgment();
    }

    private static String transactionId(OrderSide side) {
        return side == OrderSide.BUY
                ? PAPER_BUY_TRANSACTION_ID
                : PAPER_SELL_TRANSACTION_ID;
    }

    private static void requirePendingMarketOrder(Order order) {
        if (!(order.state() instanceof OrderState.PendingSubmission)) {
            throw new IllegalArgumentException("only pending order can be submitted");
        }
        if (order.type() != OrderType.MARKET) {
            throw new IllegalArgumentException("only market order can be submitted");
        }
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
