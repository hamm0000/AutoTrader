#pragma once

#include <string>
#include <vector>

namespace autotrader::strategy {

struct AnalyticsResult {
    std::string signal;
    double z_score;
    double rolling_mean;
    double rolling_stddev;
    std::string reason;
};

AnalyticsResult runZScoreStrategy(
    const std::vector<double>& close_prices,
    int window,
    double now_price
);

}  // namespace autotrader::strategy
