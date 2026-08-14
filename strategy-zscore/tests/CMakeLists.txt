include(GoogleTest)

add_executable(
    zscore_strategy_characterization_test
    zscore_strategy_characterization_test.cpp
)

target_link_libraries(
    zscore_strategy_characterization_test
    PRIVATE
        zscore_strategy
        GTest::gtest_main
)

gtest_discover_tests(zscore_strategy_characterization_test)

add_executable(
    zscore_json_contract_test
    zscore_json_contract_test.cpp
)

target_link_libraries(
    zscore_json_contract_test
    PRIVATE
        zscore_json
        GTest::gtest_main
)

gtest_discover_tests(zscore_json_contract_test)

add_executable(
    zscore_http_contract_test
    zscore_http_contract_test.cpp
)

target_link_libraries(
    zscore_http_contract_test
    PRIVATE
        zscore_http_server
        GTest::gtest_main
)

gtest_discover_tests(zscore_http_contract_test)
