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

add_library(
    zscore_strategy
    src/zscore_strategy.cpp
)

target_include_directories(
    zscore_strategy
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(zscore_strategy PUBLIC cxx_std_17)

add_library(
    zscore_json
    src/zscore_json.cpp
)

target_include_directories(
    zscore_json
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(
    zscore_json
    PUBLIC
        zscore_strategy
        nlohmann_json::nlohmann_json
)

target_compile_features(zscore_json PUBLIC cxx_std_17)

if(AUTOTRADER_STRATEGY_BUILD_TESTS)
    enable_testing()
    autotrader_strategy_fetch_test_dependencies()
    add_subdirectory(tests)
endif()
