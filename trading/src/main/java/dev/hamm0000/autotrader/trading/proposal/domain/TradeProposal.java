package dev.hamm0000.autotrader.trading.proposal.domain;


public sealed interface TradeProposal permits BuyProposal, SellProposal {

    String symbol();
}
