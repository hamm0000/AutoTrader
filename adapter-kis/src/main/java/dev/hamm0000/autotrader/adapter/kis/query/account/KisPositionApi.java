package dev.hamm0000.autotrader.adapter.kis.query.account;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQueryException;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import java.util.Objects;
import org.springframework.web.client.RestClient;

public final class KisPositionApi implements LoadPosition {

    private static final String BALANCE_PATH =
            "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String TRANSACTION_ID = "VTTC8434R";

    private final KisAccountQueryHttpClient httpClient;
    private final String accountNumber;
    private final String accountProductCode;

    public KisPositionApi(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret,
            String accountNumber,
            String accountProductCode
    ) {
        this.httpClient = new KisAccountQueryHttpClient(
                restClient,
                tokenProvider,
                appKey,
                appSecret
        );
        this.accountNumber = requireNotBlank(accountNumber, "accountNumber");
        this.accountProductCode = requireNotBlank(accountProductCode, "accountProductCode");
    }

    @Override
    public Position load(PositionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        try {
            return requestBalance()
                    .findPosition(query.symbol())
                    .orElseGet(() -> new Position(query.symbol(), 0));
        } catch (KisAccountQueryException exception) {
            throw translate(exception);
        }
    }

    private KisBalanceResponse requestBalance() {
        return httpClient.get(
                uriBuilder -> uriBuilder
                        .path(BALANCE_PATH)
                        .queryParam("CANO", accountNumber)
                        .queryParam("ACNT_PRDT_CD", accountProductCode)
                        .queryParam("AFHR_FLPR_YN", "N")
                        .queryParam("OFL_YN", "")
                        .queryParam("INQR_DVSN", "02")
                        .queryParam("UNPR_DVSN", "01")
                        .queryParam("FUND_STTL_ICLD_YN", "N")
                        .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                        .queryParam("PRCS_DVSN", "00")
                        .queryParam("CTX_AREA_FK100", "")
                        .queryParam("CTX_AREA_NK100", "")
                        .build(),
                TRANSACTION_ID,
                KisBalanceResponse.class,
                "잔고"
        );
    }

    private static PositionQueryException translate(KisAccountQueryException exception) {
        if (exception.reason() == KisAccountQueryException.Reason.INVALID_RESPONSE) {
            return PositionQueryException.invalidResponse(exception.getMessage(), exception);
        }
        return PositionQueryException.providerUnavailable(exception.getMessage(), exception);
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
