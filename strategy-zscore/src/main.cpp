#include "autotrader/strategy/zscore_http_server.hpp"

#include <httplib.h>

#include <iostream>

int main() {
    httplib::Server server;
    autotrader::strategy::registerZScoreRoutes(server);

    std::cout << "C++ zscore server started at http://0.0.0.0:9000\n";
    server.listen("0.0.0.0", 9000);
    return 0;
}
