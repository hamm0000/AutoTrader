#pragma once

#include <httplib.h>

namespace autotrader::strategy {

inline constexpr char kZScoreSignalPath[] = "/signal/zscore";
inline constexpr char kHealthPath[] = "/health";

void handleHealthRequest(
    const httplib::Request& request,
    httplib::Response& response
);

void handleZScoreRequest(
    const httplib::Request& request,
    httplib::Response& response
);

void registerZScoreRoutes(httplib::Server& server);

}  // namespace autotrader::strategy
