package dev.hamm0000.autotrader.adapter.kis.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class KisRequestPacingInterceptorTest {

    private static final Duration MINIMUM_INTERVAL = Duration.ofMillis(1_100);

    @Test
    void 연속_KIS_REST_요청은_설정된_최소_간격을_두고_시작한다() throws Exception {
        // given
        FakeTime time = new FakeTime();
        KisRequestPacingInterceptor interceptor = new KisRequestPacingInterceptor(
                MINIMUM_INTERVAL,
                time::nanoTime,
                time::sleep
        );
        List<Long> requestStartTimes = new ArrayList<>();

        // when
        intercept(interceptor, () -> requestStartTimes.add(time.nanoTime()));
        time.advance(Duration.ofMillis(400));
        intercept(interceptor, () -> requestStartTimes.add(time.nanoTime()));
        intercept(interceptor, () -> requestStartTimes.add(time.nanoTime()));

        // then
        assertThat(requestStartTimes).containsExactly(
                0L,
                MINIMUM_INTERVAL.toNanos(),
                MINIMUM_INTERVAL.multipliedBy(2).toNanos()
        );
        assertThat(time.waits()).containsExactly(
                Duration.ofMillis(700),
                MINIMUM_INTERVAL
        );
    }

    @Test
    void 동시에_들어온_KIS_REST_요청은_앞선_요청이_끝난_뒤_실행한다() throws Exception {
        // given
        KisRequestPacingInterceptor interceptor = new KisRequestPacingInterceptor(
                Duration.ofNanos(1),
                System::nanoTime,
                ignored -> { }
        );
        CountDownLatch firstRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstRequest = new CountDownLatch(1);
        CountDownLatch secondRequestAttempted = new CountDownLatch(1);
        CountDownLatch secondRequestStarted = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> interceptUnchecked(interceptor, () -> {
                firstRequestStarted.countDown();
                await(releaseFirstRequest);
            }));
            assertThat(firstRequestStarted.await(1, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> {
                secondRequestAttempted.countDown();
                interceptUnchecked(interceptor, secondRequestStarted::countDown);
            });
            assertThat(secondRequestAttempted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondRequestStarted.await(100, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirstRequest.countDown();
            get(first);
            get(second);
            assertThat(secondRequestStarted.getCount()).isZero();
        }
    }

    @Test
    void KIS_REST_요청_최소_간격은_0보다_커야_한다() {
        assertThatThrownBy(() -> new KisRequestPacingInterceptor(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minimumInterval must be greater than zero");
    }

    private static void intercept(
            KisRequestPacingInterceptor interceptor,
            Runnable onRequestStart
    ) throws Exception {
        interceptor.intercept(
                null,
                new byte[0],
                (request, body) -> {
                    onRequestStart.run();
                    return null;
                }
        );
    }

    private static void interceptUnchecked(
            KisRequestPacingInterceptor interceptor,
            Runnable onRequestStart
    ) {
        try {
            intercept(interceptor, onRequestStart);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static void get(Future<?> future) throws Exception {
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError(exception.getCause());
        }
    }

    private static final class FakeTime {

        private long currentNanos;
        private final List<Duration> waits = new ArrayList<>();

        long nanoTime() {
            return currentNanos;
        }

        void advance(Duration duration) {
            currentNanos += duration.toNanos();
        }

        void sleep(Duration duration) {
            waits.add(duration);
            advance(duration);
        }

        List<Duration> waits() {
            return List.copyOf(waits);
        }
    }
}
