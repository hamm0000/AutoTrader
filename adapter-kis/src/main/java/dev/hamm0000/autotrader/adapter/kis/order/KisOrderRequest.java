package dev.hamm0000.autotrader.adapter.kis.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderSide;

record KisOrderRequest(
        @JsonProperty("CANO") String accountNumber,
        @JsonProperty("ACNT_PRDT_CD") String accountProductCode,
        @JsonProperty("PDNO") String symbol,
        @JsonProperty("ORD_DVSN") String orderDivision,
        @JsonProperty("ORD_QTY") String quantity,
        @JsonProperty("ORD_UNPR") String unitPrice,
        @JsonProperty("EXCG_ID_DVSN_CD") String exchangeId,
        @JsonProperty("SLL_TYPE") String sellType,
        @JsonProperty("CNDT_PRIC") String conditionPrice
) {

    private static final String MARKET_ORDER = "01";
    private static final String MARKET_ORDER_PRICE = "0";
    private static final String KRX = "KRX";
    private static final String REGULAR_SELL = "01";

    static KisOrderRequest market(Order order, String accountNumber, String accountProductCode) {
        return new KisOrderRequest(
                accountNumber,
                accountProductCode,
                order.symbol(),
                MARKET_ORDER,
                Long.toString(order.quantity()),
                MARKET_ORDER_PRICE,
                KRX,
                order.side() == OrderSide.SELL ? REGULAR_SELL : "",
                ""
        );
    }
}
