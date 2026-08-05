package dev.hamm0000.autotrader.adapter.kis.auth.testdouble;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.AccessTokenIssuer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 발급 시작을 알리고, completeIssue가 호출될 때까지 발급을 대기시키는 Fake 대역
 */
public final class BlockingFakeIssuer implements AccessTokenIssuer {

    private final AccessToken token;
    private final AtomicInteger issueCount = new AtomicInteger();
    private final CountDownLatch issueStarted = new CountDownLatch(1);
    private final CountDownLatch canComplete = new CountDownLatch(1);

    public BlockingFakeIssuer(AccessToken token) {
        this.token = token;
    }

    @Override
    public AccessToken issue() {
        issueCount.incrementAndGet();
        issueStarted.countDown();
        try {
            canComplete.await();
            return token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("토큰 발급 대기 중 인터럽트 됨", e);
        }
    }

    public boolean awaitIssueStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return issueStarted.await(timeout, unit);
    }

    public void completeIssue() {
        canComplete.countDown();
    }

    public int issueCount() {
        return issueCount.get();
    }
}
