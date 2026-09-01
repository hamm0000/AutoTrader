package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.FindTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.SaveTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarSaveException;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 캘린더의 저장,조회 포트를 PostgreSQL/JPA로 구현하는 outbound adapter입니다.
 * <br/>
 * trading 모듈은 PostgreSQL이나 JPA를 알지 못하며
 * 도메인 객체와 JPA 엔티티 사이의 변환은 이 Adapter 안에서만 이뤄집니다.
 */

@Repository
public class TradingCalendarPersistenceAdapter
        implements SaveTradingCalendar, FindTradingCalendar {

    private final TradingDayRepository tradingDays;
    private final TradingCalendarRefreshMetadataRepository refreshMetadata;

    TradingCalendarPersistenceAdapter(
            TradingDayRepository tradingDays,
            TradingCalendarRefreshMetadataRepository refreshMetadata
    ) {
        this.tradingDays = Objects.requireNonNull(tradingDays, "tradingDays must not be null");
        this.refreshMetadata = Objects.requireNonNull(
                refreshMetadata,
                "refreshMetadata must not be null"
        );
    }

    @Override
    @Transactional
    public void save(TradingCalendar calendar) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        try {
            List<TradingDayEntity> entities = calendar.values().stream()
                    .map(TradingCalendarEntityMapper::toEntity)
                    .toList();
            tradingDays.saveAll(entities);

            CalendarPeriod coverage = calendar.coverage();
            TradingCalendarRefreshMetadataEntity metadata =
                    new TradingCalendarRefreshMetadataEntity(
                            coverage.from(),
                            coverage.through(),
                            oldestVerificationTime(calendar)
                    );

            // flush 없이 커밋 시점까지 미루면 실패가 이 메서드 밖에서 일어나 아래의 catch가 못 잡음
            refreshMetadata.saveAndFlush(metadata);
        } catch (DataAccessException exception) {
            throw new TradingCalendarSaveException(
                    "거래 캘린더를 저장하지 못했습니다",
                    exception
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingCalendar> find(CalendarPeriod period) {
        Objects.requireNonNull(period, "period must not be null");

        List<TradingDayEntity> entities;
        try {
            entities = tradingDays.findByTradeDateBetweenOrderByTradeDateAsc(
                    period.from(),
                    period.through()
            );
        } catch (DataAccessException exception) {
            throw new TradingCalendarQueryException(
                    "거래 캘린더를 조회하지 못했습니다",
                    exception
            );
        }

        // 요청 기간 중에 하루라도 없으면 추론하지 않고 빈 결과 반환
        if (entities.size() != period.dayCount()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new TradingCalendar(
                    period,
                    entities.stream()
                            .map(TradingCalendarEntityMapper::toDomain)
                            .toList()
            ));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new TradingCalendarQueryException(
                    "저장된 거래 캘린더가 유효하지 않습니다",
                    exception
            );
        }
    }

    // 범위 내 가장 오래된 VerificationTime을 이용해 캘린더 신선도를 보수적으로 표현
    private static Instant oldestVerificationTime(TradingCalendar calendar) {
        return calendar.values().stream()
                .map(TradingDay::verifiedAt)
                .min(Instant::compareTo)
                .orElseThrow();
    }
}
