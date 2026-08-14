#include "autotrader/strategy/zscore_strategy.hpp"

#include <cmath>
#include <numeric>
#include <stdexcept>
#include <vector>

namespace autotrader::strategy {
namespace {

double round2(double value) {
    return std::round(value * 100.0) / 100.0;
}

double calculateMean(const std::vector<double>& values) {
    if (values.empty()) {
        throw std::invalid_argument("values is empty");
    }

    double sum = std::accumulate(values.begin(), values.end(), 0.0);
    return sum / static_cast<double>(values.size());
}

double calculateStdDev(const std::vector<double>& values, double mean) {
    if (values.empty()) {
        throw std::invalid_argument("values is empty");
    }

    double sum = 0.0;
    for (double v : values) {
        double diff = v - mean;
        sum += diff * diff;
    }

    // population stddev
    return std::sqrt(sum / static_cast<double>(values.size()));
}

}  // namespace

AnalyticsResult runZScoreStrategy(
    const std::vector<double>& close_prices,
    int window,
    double now_price
) {
    if (window < 2) {
        throw std::invalid_argument("window must be >= 2");
    }

    if (static_cast<int>(close_prices.size()) < window) {
        throw std::invalid_argument("close_prices size must be >= window");
    }

    std::vector<double> rolling_window(
        close_prices.end() - window,
        close_prices.end()
    );

    const double mean = calculateMean(rolling_window);
    const double stddev = calculateStdDev(rolling_window, mean);

    AnalyticsResult result{};

    result.rolling_mean = round2(mean);
    result.rolling_stddev = round2(stddev);

    if (stddev == 0.0) {
        result.z_score = 0.0;
        result.signal = "HOLD";
        result.reason = "Rolling stddev is zero";
        return result;
    }

    const double z = (now_price - mean) / stddev;
    result.z_score = round2(z);

    if (z >= 2.0) {
        result.signal = "SELL";
        result.reason = "Z-score above upper threshold";
    } else if (z <= -2.0) {
        result.signal = "BUY";
        result.reason = "Z-score below lower threshold";
    } else {
        result.signal = "HOLD";
        result.reason = "Z-score within normal range";
    }

    return result;
}

}  // namespace autotrader::strategy
