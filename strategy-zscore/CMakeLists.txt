cmake_minimum_required(VERSION 3.24)

project(
    autotrader_zscore_strategy
    VERSION 0.1.0
    LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(
    AUTOTRADER_STRATEGY_BUILD_TESTS
    "Build the Z-score strategy characterization tests"
    ON
)

include(cmake/Dependencies.cmake)

autotrader_strategy_fetch_runtime_dependencies()

if(AUTOTRADER_STRATEGY_BUILD_TESTS)
    enable_testing()
    autotrader_strategy_fetch_test_dependencies()
    add_subdirectory(tests)
endif()
