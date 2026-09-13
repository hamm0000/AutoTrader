package dev.hamm0000.autotrader.trading.proposal.domain;

import java.math.BigDecimal;

public sealed interface TradeProposal permits BuyProposal, SellProposal {

    String symbol();

    long quantity();

    BigDecimal referencePrice();
}
