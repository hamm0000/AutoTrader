package dev.hamm0000.autotrader.adapter.strategy.zscore.http;

import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.EvaluateZScoreStrategy;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationException;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationRequest;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationResult;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

public final class ZScoreHttpStrategyAdapter implements EvaluateZScoreStrategy {

    private static final String SIGNAL_PATH = "/signal/zscore";

    private final RestClient restClient;

    public ZScoreHttpStrategyAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public ZScoreEvaluationResult evaluate(ZScoreEvaluationRequest request) {
        ZScoreHttpResponse response = requestEvaluation(request);
        if (response == null) {
            throw new ZScoreEvaluationException(
                    ZScoreEvaluationException.Reason.INVALID_RESPONSE,
                    "Z-score 서버 응답이 비어 있습니다"
            );
        }
        return response.toResult();
    }

    private ZScoreHttpResponse requestEvaluation(ZScoreEvaluationRequest request) {
        try {
            return restClient.post()
                    .uri(SIGNAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ZScoreHttpRequest.from(request))
                    .retrieve()
                    .body(ZScoreHttpResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ZScoreEvaluationException(
                        ZScoreEvaluationException.Reason.INVALID_REQUEST,
                        "Z-score 서버가 요청을 거부했습니다",
                        exception
                );
            }
            throw serverUnavailable(exception);
        } catch (RestClientException exception) {
            if (hasCause(exception, HttpMessageConversionException.class)) {
                throw new ZScoreEvaluationException(
                        ZScoreEvaluationException.Reason.INVALID_RESPONSE,
                        "Z-score 서버 응답을 해석할 수 없습니다",
                        exception
                );
            }
            throw serverUnavailable(exception);
        }
    }

    private static ZScoreEvaluationException serverUnavailable(Throwable cause) {
        return new ZScoreEvaluationException(
                ZScoreEvaluationException.Reason.SERVER_UNAVAILABLE,
                "Z-score 서버를 사용할 수 없습니다",
                cause
        );
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> causeType) {
        Throwable cause = failure;
        while (cause != null) {
            if (causeType.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
