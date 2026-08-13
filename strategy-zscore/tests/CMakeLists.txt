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
