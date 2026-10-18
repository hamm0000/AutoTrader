package dev.hamm0000.autotrader.adapter.kis.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * KIS 모의투자 REST API의 초당 호출 제한을 지키기 위해 요청을 하나씩 보내고,
 * 직전 요청이 시작된 뒤 최소 간격이 지나야 다음 요청을 보냅니다.
 * <br/>
 * 호출하는 곳마다 직접 기다리게 하면 새 API를 추가할 때 빠뜨릴 수 있으므로,
 * 실제 HTTP 요청이 나가기 직전인 Interceptor에서 한 번에 처리합니다.
 */
public final class KisRequestPacingInterceptor implements ClientHttpRequestInterceptor {

    // 시스템 시간이 바뀌어도 요청 간격 계산에는 영향이 없도록 nanoTime 기준으로 저장
    private final long minimumIntervalNanos;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;

    // true는 먼저 기다린 요청부터 lock을 받게 해서 특정 요청이 계속 밀리지 않도록 함
    private final ReentrantLock requestLock = new ReentrantLock(true);

    // 첫 요청은 바로 보내고, 두 번째 요청부터 직전 요청 시작 시각과 비교
    private boolean requestStarted;
    private long lastRequestStartNanos;

    public KisRequestPacingInterceptor(Duration minimumInterval) {
        this(
                minimumInterval,
                System::nanoTime,
                duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos())
        );
    }

    KisRequestPacingInterceptor(
            Duration minimumInterval,
            LongSupplier nanoTime,
            Sleeper sleeper
    ) {
        Objects.requireNonNull(minimumInterval, "minimumInterval must not be null");
        long intervalNanos = minimumInterval.toNanos();
        if (intervalNanos <= 0) {
            throw new IllegalArgumentException("minimumInterval must be greater than zero");
        }
        this.minimumIntervalNanos = intervalNanos;
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper must not be null");
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        // lock을 HTTP 응답까지 유지해서 같은 프로세스의 KIS 요청이 서로 겹치지 않게 함
        acquireLock();
        try {
            awaitNextRequest();

            // 응답 완료 시각이 아니라 요청 시작 시각을 기준으로 다음 요청 간격 계산
            lastRequestStartNanos = nanoTime.getAsLong();
            requestStarted = true;
            return execution.execute(request, body);
        } catch (InterruptedException exception) {
            // interrupt를 삼키지 않고 복구해 상위 계층에서도 작업 중단을 알 수 있게 함
            Thread.currentThread().interrupt();
            throw interrupted(exception);
        } finally {
            requestLock.unlock();
        }
    }

    private void acquireLock() throws InterruptedIOException {
        try {
            requestLock.lockInterruptibly();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw interrupted(exception);
        }
    }

    private void awaitNextRequest() throws InterruptedException {
        if (!requestStarted) {
            return;
        }

        long elapsedNanos = Math.max(0, nanoTime.getAsLong() - lastRequestStartNanos);
        long remainingNanos = minimumIntervalNanos - elapsedNanos;
        if (remainingNanos > 0) {
            sleeper.sleep(Duration.ofNanos(remainingNanos));
        }
    }

    private static InterruptedIOException interrupted(InterruptedException cause) {
        InterruptedIOException failure =
                new InterruptedIOException("KIS REST 요청 간격 대기가 중단되었습니다");
        failure.initCause(cause);
        return failure;
    }

    @FunctionalInterface
    interface Sleeper {

        // 테스트에서는 실제로 기다리지 않고 시간 흐름만 확인할 수 있도록 분리
        void sleep(Duration duration) throws InterruptedException;
    }
}
