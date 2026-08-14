#include "autotrader/strategy/zscore_http_server.hpp"

#include <gtest/gtest.h>
#include <httplib.h>
#include <nlohmann/json.hpp>

#include <string>

namespace autotrader::strategy {
namespace {

using json = nlohmann::json;

httplib::Request requestWith(const json& body) {
    httplib::Request request;
    request.body = body.dump();
    return request;
}

// 올바른 JSON 요청이 주어졌을 때 / Z-score HTTP 요청을 처리하면 / 기존 형식의 200 응답을 반환한다.
TEST(ZScoreHttpResponse, ReturnsAnalyticsResultWithStatusOk) {
    const httplib::Request request = requestWith({
        {"symbol", "005930"},
        {"window", 2},
        {"close_prices", {99.0, 101.0}},
        {"now_price", 102.0}
    });
    httplib::Response response;

    handleZScoreRequest(request, response);

    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.get_header_value("Content-Type"), "application/json");
    EXPECT_EQ(json::parse(response.body), json({
        {"signal", "SELL"},
        {"z_score", 2.0},
        {"rolling_mean", 100.0},
        {"rolling_stddev", 1.0},
        {"reason", "Z-score above upper threshold"}
    }));
}

// 필수 필드가 없을 때 / Z-score HTTP 요청을 처리하면 / 기존 오류 형식의 400 응답을 반환한다.
TEST(ZScoreHttpResponse, ReturnsBadRequestWhenRequiredFieldIsMissing) {
    const httplib::Request request = requestWith({
        {"symbol", "005930"},
        {"window", 2},
        {"close_prices", {99.0, 101.0}}
    });
    httplib::Response response;

    handleZScoreRequest(request, response);

    EXPECT_EQ(response.status, 400);
    EXPECT_EQ(response.get_header_value("Content-Type"), "application/json");
    EXPECT_EQ(json::parse(response.body), json({
        {"error", "now_price is required"}
    }));
}

// 종가 개수가 window보다 적을 때 / Z-score HTTP 요청을 처리하면 / 기존 오류 메시지의 400 응답을 반환한다.
TEST(ZScoreHttpResponse, ReturnsBadRequestWhenClosePricesAreInsufficient) {
    const httplib::Request request = requestWith({
        {"symbol", "005930"},
        {"window", 3},
        {"close_prices", {99.0, 101.0}},
        {"now_price", 102.0}
    });
    httplib::Response response;

    handleZScoreRequest(request, response);

    EXPECT_EQ(response.status, 400);
    EXPECT_EQ(json::parse(response.body), json({
        {"error", "close_prices size must be >= window"}
    }));
}


}  // namespace
}  // namespace autotrader::strategy
