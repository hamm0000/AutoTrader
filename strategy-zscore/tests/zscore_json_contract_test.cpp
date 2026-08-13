#include "autotrader/strategy/zscore_json.hpp"

#include <gtest/gtest.h>
#include <nlohmann/json.hpp>

#include <stdexcept>
#include <string>
#include <vector>

namespace autotrader::strategy {
namespace {

using json = nlohmann::json;

json validRequest() {
    return {
        {"symbol", "005930"},
        {"window", 3},
        {"close_prices", {99.0, 100.0, 101.0}},
        {"now_price", 102.0}
    };
}

void expectInvalidRequest(
    const json& request,
    const std::string& expected_message
) {
    try {
        parseZScoreRequest(request);
        FAIL() << "Expected std::invalid_argument";
    } catch (const std::invalid_argument& exception) {
        EXPECT_EQ(exception.what(), expected_message);
    } catch (...) {
        FAIL() << "Expected std::invalid_argument";
    }
}

// 필수 요청 필드가 올바를 때 / JSON 요청을 변환하면 / 전략 계산에 필요한 값을 반환한다.
TEST(ZScoreJsonRequest, ParsesValidRequest) {
    const ZScoreRequest request = parseZScoreRequest(validRequest());

    EXPECT_EQ(request.symbol, "005930");
    EXPECT_EQ(request.window, 3);
    EXPECT_EQ(request.close_prices, (std::vector<double>{99.0, 100.0, 101.0}));
    EXPECT_DOUBLE_EQ(request.now_price, 102.0);
}

// 종가 개수가 window보다 많을 때 / JSON 요청을 검증하면 / 기존 계약대로 요청을 허용한다.
TEST(ZScoreJsonRequest, AcceptsMoreClosePricesThanWindow) {
    json body = validRequest();
    body["window"] = 2;

    const ZScoreRequest request = parseZScoreRequest(body);

    EXPECT_EQ(request.close_prices.size(), 3U);
}

// symbol이 없거나 문자열이 아닐 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsMissingOrInvalidSymbol) {
    json missing = validRequest();
    missing.erase("symbol");
    json invalid = validRequest();
    invalid["symbol"] = 5930;

    expectInvalidRequest(missing, "symbol is required");
    expectInvalidRequest(invalid, "symbol is required");
}

// window가 없거나 정수가 아닐 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsMissingOrInvalidWindow) {
    json missing = validRequest();
    missing.erase("window");
    json invalid = validRequest();
    invalid["window"] = 3.5;

    expectInvalidRequest(missing, "window is required");
    expectInvalidRequest(invalid, "window is required");
}

// close_prices가 없거나 배열이 아닐 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsMissingOrInvalidClosePrices) {
    json missing = validRequest();
    missing.erase("close_prices");
    json invalid = validRequest();
    invalid["close_prices"] = "99, 100, 101";

    expectInvalidRequest(missing, "close_prices is required");
    expectInvalidRequest(invalid, "close_prices is required");
}

// now_price가 없거나 숫자가 아닐 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsMissingOrInvalidCurrentPrice) {
    json missing = validRequest();
    missing.erase("now_price");
    json invalid = validRequest();
    invalid["now_price"] = "102";

    expectInvalidRequest(missing, "now_price is required");
    expectInvalidRequest(invalid, "now_price is required");
}

// window가 2보다 작을 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsWindowSmallerThanTwo) {
    json body = validRequest();
    body["window"] = 1;

    expectInvalidRequest(body, "window must be >= 2");
}

// 종가 배열에 숫자가 아닌 값이 있을 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsNonNumericClosePrice) {
    json body = validRequest();
    body["close_prices"] = {99.0, "100", 101.0};

    expectInvalidRequest(body, "close_prices must contain only numbers");
}

// 종가 개수가 window보다 적을 때 / JSON 요청을 검증하면 / 기존 오류 메시지로 거부한다.
TEST(ZScoreJsonRequest, RejectsFewerClosePricesThanWindow) {
    json body = validRequest();
    body["window"] = 4;

    expectInvalidRequest(body, "close_prices size must be >= window");
}

// 전략 계산 결과가 주어졌을 때 / JSON 응답으로 변환하면 / 기존 응답 필드와 값을 반환한다.
TEST(ZScoreJsonResponse, SerializesAnalyticsResult) {
    const AnalyticsResult result{
        "SELL",
        2.14,
        100.0,
        1.25,
        "Z-score above upper threshold"
    };

    const json response = toJson(result);

    EXPECT_EQ(response, json({
        {"signal", "SELL"},
        {"z_score", 2.14},
        {"rolling_mean", 100.0},
        {"rolling_stddev", 1.25},
        {"reason", "Z-score above upper threshold"}
    }));
}

}  // namespace
}  // namespace autotrader::strategy
