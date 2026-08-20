package dev.hamm0000.autotrader.adapter.kis.query;

import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.invalidResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * KIS 주식현재가 일자별 조회 응답의 output 배열 항목
 *
 * <p>실제 응답엔 최고가,거래량 등 필드가 더 있지만, {@link DailyClosingPrice}가
 * 필요로 하는 거래일과 종가만 가져온다
 */
record KisDailyClosingPriceItem(
        @JsonProperty("stck_bsop_date") String tradingDate,
        @JsonProperty("stck_clpr") String closingPrice
) {

    DailyClosingPrice toDailyClosingPrice() {
        if (tradingDate == null || tradingDate.isBlank()
                || closingPrice == null || closingPrice.isBlank()) {
            throw invalidResponse("KIS 최근 종가 응답에 필수 필드가 없습니다");
        }

        try {
            return new DailyClosingPrice(
                    LocalDate.parse(tradingDate, DateTimeFormatter.BASIC_ISO_DATE),
                    new BigDecimal(closingPrice)
            );
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw invalidResponse("KIS 최근 종가 응답의 날짜 또는 가격이 올바르지 않습니다", exception);
        }
    }
}
