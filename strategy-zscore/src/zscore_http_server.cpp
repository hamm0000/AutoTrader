#include "autotrader/strategy/zscore_http_server.hpp"

#include "autotrader/strategy/zscore_json.hpp"
#include "autotrader/strategy/zscore_strategy.hpp"

#include <nlohmann/json.hpp>

#include <exception>

namespace autotrader::strategy {

void handleZScoreRequest(
    const httplib::Request& request,
    httplib::Response& response
) {
    try {
        const nlohmann::json body = nlohmann::json::parse(request.body);
        const ZScoreRequest strategy_request = parseZScoreRequest(body);

        const AnalyticsResult result = runZScoreStrategy(
            strategy_request.close_prices,
            strategy_request.window,
            strategy_request.now_price
        );

        response.set_content(toJson(result).dump(), "application/json");
        response.status = 200;
    } catch (const std::exception& exception) {
        const nlohmann::json error = {
            {"error", exception.what()}
        };
        response.set_content(error.dump(), "application/json");
        response.status = 400;
    }
}

void registerZScoreRoutes(httplib::Server& server) {
    server.Post(kZScoreSignalPath, handleZScoreRequest);
}

}  // namespace autotrader::strategy
