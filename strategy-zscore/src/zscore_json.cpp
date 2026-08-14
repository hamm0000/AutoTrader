#include "autotrader/strategy/zscore_json.hpp"

#include <nlohmann/json.hpp>

#include <stdexcept>
#include <string>
#include <vector>

namespace autotrader::strategy {

ZScoreRequest parseZScoreRequest(const nlohmann::json& request) {
    if (!request.contains("symbol") || !request["symbol"].is_string()) {
        throw std::invalid_argument("symbol is required");
    }
    if (!request.contains("window") || !request["window"].is_number_integer()) {
        throw std::invalid_argument("window is required");
    }
    if (!request.contains("close_prices") || !request["close_prices"].is_array()) {
        throw std::invalid_argument("close_prices is required");
    }
    if (!request.contains("now_price") || !request["now_price"].is_number()) {
        throw std::invalid_argument("now_price is required");
    }

    const std::string symbol = request["symbol"].get<std::string>();
    const int window = request["window"].get<int>();
    const double now_price = request["now_price"].get<double>();

    if (window < 2) {
        throw std::invalid_argument("window must be >= 2");
    }

    std::vector<double> close_prices;
    for (const auto& value : request["close_prices"]) {
        if (!value.is_number()) {
            throw std::invalid_argument("close_prices must contain only numbers");
        }
        close_prices.push_back(value.get<double>());
    }

    if (static_cast<int>(close_prices.size()) < window) {
        throw std::invalid_argument("close_prices size must be >= window");
    }

    return ZScoreRequest{
        symbol,
        window,
        close_prices,
        now_price
    };
}

nlohmann::json toJson(const AnalyticsResult& result) {
    return {
        {"signal", result.signal},
        {"z_score", result.z_score},
        {"rolling_mean", result.rolling_mean},
        {"rolling_stddev", result.rolling_stddev},
        {"reason", result.reason}
    };
}

}  // namespace autotrader::strategy
