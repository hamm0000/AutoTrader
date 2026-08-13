#include "autotrader/strategy/zscore_strategy.hpp"

#include <gtest/gtest.h>

#include <stdexcept>
#include <vector>

namespace autotrader::strategy {
namespace {

// 반올림 전 Z-score가 2.0 이상일 때 / 매매 신호를 판단하면 / SELL을 반환한다.
TEST(ZScoreStrategy, SellsWhenZScoreIsAtLeastTwo) {
    const AnalyticsResult result = runZScoreStrategy({99.0, 101.0}, 2, 102.0);

    EXPECT_EQ(result.signal, "SELL");
    EXPECT_DOUBLE_EQ(result.z_score, 2.0);
}

// 반올림 전 Z-score가 -2.0 이하일 때 / 매매 신호를 판단하면 / BUY를 반환한다.
TEST(ZScoreStrategy, BuysWhenZScoreIsAtMostMinusTwo) {
    const AnalyticsResult result = runZScoreStrategy({99.0, 101.0}, 2, 98.0);

    EXPECT_EQ(result.signal, "BUY");
    EXPECT_DOUBLE_EQ(result.z_score, -2.0);
}

// 반올림 전 Z-score가 -2.0과 2.0 사이일 때 / 매매 신호를 판단하면 / HOLD를 반환한다.
TEST(ZScoreStrategy, HoldsWhenZScoreIsBetweenThresholds) {
    const AnalyticsResult result = runZScoreStrategy({99.0, 101.0}, 2, 101.5);

    EXPECT_EQ(result.signal, "HOLD");
    EXPECT_DOUBLE_EQ(result.z_score, 1.5);
}

// 표준편차가 0.0일 때 / 매매 신호를 판단하면 / Z-score 0.0과 HOLD를 반환한다.
TEST(ZScoreStrategy, HoldsWhenStandardDeviationIsZero) {
    const AnalyticsResult result = runZScoreStrategy(
        {10.0, 10.0, 10.0},
        3,
        100.0
    );

    EXPECT_EQ(result.signal, "HOLD");
    EXPECT_DOUBLE_EQ(result.z_score, 0.0);
}

// 종가가 window보다 많을 때 / Z-score를 계산하면 / 가장 최근 window개 종가만 사용한다.
TEST(ZScoreStrategy, UsesMostRecentClosePricesWithinWindow) {
    const AnalyticsResult result = runZScoreStrategy(
        {10000.0, 99.0, 101.0},
        2,
        102.0
    );

    EXPECT_DOUBLE_EQ(result.rolling_mean, 100.0);
    EXPECT_DOUBLE_EQ(result.rolling_stddev, 1.0);
    EXPECT_DOUBLE_EQ(result.z_score, 2.0);
    EXPECT_EQ(result.signal, "SELL");
}


// 계산 결과를 반환할 때 / 평균, 표준편차, Z-score를 출력하면 / 소수 둘째 자리로 반올림한다.
TEST(ZScoreStrategy, RoundsReportedValuesToTwoDecimalPlaces) {
    const AnalyticsResult result = runZScoreStrategy(
        {1.0, 2.0, 3.0},
        3,
        4.0
    );

    EXPECT_DOUBLE_EQ(result.rolling_mean, 2.0);
    EXPECT_DOUBLE_EQ(result.rolling_stddev, 0.82);
    EXPECT_DOUBLE_EQ(result.z_score, 2.45);
}

// 반올림 전 Z-score는 2.0 미만이지만 출력값은 2.00일 때 / 매매 신호를 판단하면 / 반올림 전 값을 기준으로 HOLD를 반환한다.
TEST(ZScoreStrategy, UsesRawZScoreForUpperThresholdDecision) {
    const AnalyticsResult result = runZScoreStrategy(
        {99.0, 101.0},
        2,
        101.999
    );

    EXPECT_DOUBLE_EQ(result.z_score, 2.0);
    EXPECT_EQ(result.signal, "HOLD");
}

// 반올림 전 Z-score는 -2.0 초과지만 출력값은 -2.00일 때 / 매매 신호를 판단하면 / 반올림 전 값을 기준으로 HOLD를 반환한다.
TEST(ZScoreStrategy, UsesRawZScoreForLowerThresholdDecision) {
    const AnalyticsResult result = runZScoreStrategy(
        {99.0, 101.0},
        2,
        98.001
    );

    EXPECT_DOUBLE_EQ(result.z_score, -2.0);
    EXPECT_EQ(result.signal, "HOLD");
}

// window가 2보다 작다면 / 잘못된 입력으로 거부한다.
TEST(ZScoreInput, RejectsWindowSmallerThanTwo) {
    EXPECT_THROW(
        runZScoreStrategy({1.0}, 1, 2.0),
        std::invalid_argument
    );
}

// 종가 개수가 window보다 작다면 / 잘못된 입력으로 거부한다.
TEST(ZScoreInput, RejectsFewerClosePricesThanWindow) {
    EXPECT_THROW(
        runZScoreStrategy({1.0, 2.0}, 3, 3.0),
        std::invalid_argument
    );
}

}  // namespace
}  // namespace autotrader::strategy
