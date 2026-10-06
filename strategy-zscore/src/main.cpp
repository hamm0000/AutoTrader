#include "autotrader/strategy/zscore_http_server.hpp"

#include <httplib.h>

#include <cstdlib>
#include <iostream>

int main() {
    httplib::Server server;
    autotrader::strategy::registerZScoreRoutes(server);

    if (!server.bind_to_port("0.0.0.0", 9000)) {
        std::cerr << "failed to bind zscore server to 0.0.0.0:9000"
                  << std::endl;
        return EXIT_FAILURE;
    }

    std::cout << "C++ zscore server started at http://0.0.0.0:9000"
              << std::endl;

    return server.listen_after_bind() ? EXIT_SUCCESS : EXIT_FAILURE;
}
