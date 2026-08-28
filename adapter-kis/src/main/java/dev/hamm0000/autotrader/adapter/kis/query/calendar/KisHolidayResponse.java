package dev.hamm0000.autotrader.adapter.kis.query.calendar;

import static dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatusQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatusQueryException.providerUnavailable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatusObservation;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDayStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record KisHolidayResponse(
        @JsonProperty("rt_cd") String returnCode,
        List<KisHolidayItem> output
) {

    List<TradingDayStatusObservation> toTradingDayStatuses() {
        if (returnCode == null || returnCode.isBlank()) {
            throw invalidResponse("KIS 국내휴장일 응답에 rt_cd가 없습니다");
        }
        if (!returnCode.equals("0")) {
            throw providerUnavailable("KIS가 국내휴장일 조회 요청을 처리하지 못했습니다");
        }
        if (output == null || output.isEmpty()) {
            throw invalidResponse("KIS 국내휴장일 응답에 output이 없습니다");
        }

        List<TradingDayStatusObservation> statuses = new ArrayList<>();
        for (KisHolidayItem item : output) {
            if (item == null) {
                throw invalidResponse("KIS 국내휴장일 응답에 비어 있는 항목이 있습니다");
            }
            statuses.add(item.toTradingDayStatus());
        }
        return List.copyOf(statuses);
    }
}

/**
 * KIS 응답의 tr_day_yn(거래일여부)은 주말에도 Y일 수 있으므로,
 * 거래 캘린더의 OPEN/CLOSED는 실제 개장일 여부인 opnd_yn만으로 판단합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KisHolidayItem(
        @JsonProperty("bass_dt") String baseDate, // 해당 날자
        @JsonProperty("opnd_yn") String marketOpen // bool 아니고 y or n
) {

    TradingDayStatusObservation toTradingDayStatus() {
        if (baseDate == null || baseDate.isBlank()
                || marketOpen == null || marketOpen.isBlank()) {
            throw invalidResponse("KIS 국내휴장일 응답에 필수 필드가 없습니다");
        }
        if (!marketOpen.equals("Y") && !marketOpen.equals("N")) {
            throw invalidResponse("KIS 국내휴장일 응답의 개장일 여부가 올바르지 않습니다");
        }

        try {
            return new TradingDayStatusObservation(
                    LocalDate.parse(baseDate, DateTimeFormatter.BASIC_ISO_DATE),
                    marketOpen.equals("Y")
                            ? TradingDayStatus.OPEN
                            : TradingDayStatus.CLOSED
            );
        } catch (DateTimeParseException exception) {
            throw invalidResponse("KIS 국내휴장일 응답의 날짜가 올바르지 않습니다", exception);
        }
    }
}
