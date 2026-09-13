package dev.hamm0000.autotrader.app.live.kis.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.kis.paper.account")
public record KisPaperAccountProperties(
        String accountNumber,
        String accountProductCode
) {

    public void validate() {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("KIS 모의투자 계좌번호가 필요합니다");
        }
        if (accountProductCode == null || accountProductCode.isBlank()) {
            throw new IllegalStateException("KIS 모의투자 계좌 상품코드가 필요합니다");
        }
    }

    @Override
    public String toString() {
        return "KisPaperAccountProperties[accountNumber=***, accountProductCode=***]";
    }
}
