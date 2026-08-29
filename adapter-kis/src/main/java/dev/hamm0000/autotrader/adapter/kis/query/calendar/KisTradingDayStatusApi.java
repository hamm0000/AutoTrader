package dev.hamm0000.autotrader.adapter.kis.query.calendar;

import static dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusQueryException.invalidResponse;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.LoadTradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusObservation;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.web.client.RestClient;

/**
 * 국내휴장일 API는 모의투자에서 지원되지 않으므로 실행할 때,
 * 실전투자 인증을 사용해야 합니다.
 * <br/>
 * KIS의 1일 1회 호출 권고를 보수적으로 적용해 주 당 한 번만 요청하며,
 * 응답이 요청 기간을 완전히 포함하지 않으면 잘못된 응답으로 처리합니다.
 */
public final class KisTradingDayStatusApi implements LoadTradingDayStatuses {

    private static final String HOLIDAY_PATH =
            "/uapi/domestic-stock/v1/quotations/chk-holiday";
    private static final String TRANSACTION_ID = "CTCA0903R";

    private final KisCalendarQueryHttpClient httpClient;

    public KisTradingDayStatusApi(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret
    ) {
        this.httpClient = new KisCalendarQueryHttpClient(
                restClient,
                tokenProvider,
                appKey,
                appSecret
        );
    }

    @Override
    public TradingDayStatuses load(CalendarPeriod period) {
        Objects.requireNonNull(period, "period must not be null");
        KisHolidayResponse response = request(period);
        return requestedStatuses(period, response.toTradingDayStatuses());
    }

    private KisHolidayResponse request(CalendarPeriod period) {
        return httpClient.get(
                uriBuilder -> uriBuilder
                        .path(HOLIDAY_PATH)
                        .queryParam(
                                "BASS_DT",
                                period.from().format(DateTimeFormatter.BASIC_ISO_DATE)
                        )
                        // 최초 조회에 필요한 빈 값이며, 응답의 연속조회 키는 사용하지 않음
                        .queryParam("CTX_AREA_FK", "")
                        .queryParam("CTX_AREA_NK", "")
                        .build(),
                TRANSACTION_ID,
                KisHolidayResponse.class,
                "국내휴장일"
        );
    }

    private static TradingDayStatuses requestedStatuses(
            CalendarPeriod period,
            List<TradingDayStatusObservation> statuses
    ) {
        List<TradingDayStatusObservation> requested = statuses.stream()
                .filter(status -> period.contains(status.date()))
                .sorted(Comparator.comparing(TradingDayStatusObservation::date))
                .toList();
        try {
            return new TradingDayStatuses(period, requested);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(
                    "KIS 국내휴장일 응답이 요청한 날짜 범위를 완전히 포함하지 않습니다",
                    exception
            );
        }
    }
}
