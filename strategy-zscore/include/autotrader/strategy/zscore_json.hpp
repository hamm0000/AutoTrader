#pragma once

#include "autotrader/strategy/zscore_strategy.hpp"

#include <nlohmann/json_fwd.hpp>

#include <string>
#include <vector>

namespace autotrader::strategy {

struct ZScoreRequest {
    std::string symbol;
    int window;
    std::vector<double> close_prices;
    double now_price;
};

ZScoreRequest parseZScoreRequest(const nlohmann::json& request);

nlohmann::json toJson(const AnalyticsResult& result);

}  // namespace autotrader::strategy
