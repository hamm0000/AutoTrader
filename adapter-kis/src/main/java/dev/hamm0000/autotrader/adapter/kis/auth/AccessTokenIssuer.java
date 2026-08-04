package dev.hamm0000.autotrader.adapter.kis.auth;

// 캐시를 조회하지 않으며 호출할 때마다 새로 발급합니다.
public interface AccessTokenIssuer {

    AccessToken issue();
}
