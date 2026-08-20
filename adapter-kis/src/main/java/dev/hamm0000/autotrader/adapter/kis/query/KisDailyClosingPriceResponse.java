package dev.hamm0000.autotrader.adapter.kis.query;

import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.insufficientData;
import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.providerUnavailable;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record KisDailyClosingPriceResponse(
        @JsonProperty("rt_cd") String returnCode,
        List<KisDailyClosingPriceItem> output
) {

    RecentClosingPrices toRecentClosingPrices(RecentClosingPricesQuery query) {
        if (returnCode == null || returnCode.isBlank()) {
            throw invalidResponse("KIS 최근 종가 응답에 rt_cd가 없습니다");
        }
        if (!returnCode.equals("0")) {
            throw providerUnavailable("KIS가 최근 종가 조회 요청을 처리하지 못했습니다");
        }
        if (output == null) {
            throw invalidResponse("KIS 최근 종가 응답에 output이 없습니다");
        }

        List<DailyClosingPrice> completedPrices = new ArrayList<>();
        for (KisDailyClosingPriceItem item : output) {
            if (item == null) {
                throw invalidResponse("KIS 최근 종가 응답에 비어 있는 항목이 있습니다");
            }
            DailyClosingPrice closingPrice = item.toDailyClosingPrice();
            if (closingPrice.tradingDate().isBefore(query.endDateExclusive())) {
                completedPrices.add(closingPrice);
            }
        }

        completedPrices.sort(Comparator.comparing(DailyClosingPrice::tradingDate).reversed());
        if (completedPrices.size() < query.count()) {
            throw insufficientData("요청한 개수만큼 완료된 최근 종가를 조회하지 못했습니다");
        }

        List<DailyClosingPrice> selectedPrices = new ArrayList<>(
                completedPrices.subList(0, query.count())
        );
        selectedPrices.sort(Comparator.comparing(DailyClosingPrice::tradingDate));

        try {
            return new RecentClosingPrices(selectedPrices);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse("KIS 최근 종가 응답의 거래일이 올바르지 않습니다", exception);
        }
    }
}
