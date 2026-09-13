package dev.hamm0000.autotrader.adapter.kis.auth;

import java.util.Optional;

/**
 * 애플리케이션 재기동 사이에(거의 로컬에서만 해당) KIS Access Token을 재사용하기 위한 저장소입니다.
 */
public interface AccessTokenStore {

    Optional<AccessToken> find();

    void save(AccessToken token);

    static AccessTokenStore none() {
        return NoOpAccessTokenStore.INSTANCE;
    }

    enum NoOpAccessTokenStore implements AccessTokenStore {
        INSTANCE;

        @Override
        public Optional<AccessToken> find() {
            return Optional.empty();
        }

        @Override
        public void save(AccessToken token) {
        }
    }
}
