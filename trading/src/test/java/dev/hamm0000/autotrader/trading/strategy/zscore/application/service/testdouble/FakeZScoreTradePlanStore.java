package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.FindZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FakeZScoreTradePlanStore implements FindZScoreTradePlan, SaveZScoreTradePlan {

    private final Map<ZScoreTradePlanningCommand, ZScoreTradePlan> plans = new LinkedHashMap<>();
    private RuntimeException saveFailure;
    private RuntimeException findFailure;
    private int failingFindAttempt;
    private ZScoreTradePlanningCommand concurrentCommand;
    private ZScoreTradePlan concurrentPlan;
    private SaveZScoreTradePlanCommand command;
    private int findCount;
    private int saveCount;

    @Override
    public Optional<ZScoreTradePlan> findByPlanningCommand(
            ZScoreTradePlanningCommand command
    ) {
        findCount++;
        if (findFailure != null && findCount == failingFindAttempt) {
            throw findFailure;
        }
        return plans.entrySet().stream()
                .filter(entry -> sameRequest(entry.getKey(), command))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @Override
    public void save(SaveZScoreTradePlanCommand command) {
        saveCount++;
        this.command = command;
        if (saveFailure != null) {
            // DB 유니크 제약 충돌 재현
            if (concurrentPlan != null) {
                plans.put(concurrentCommand, concurrentPlan);
            }
            throw saveFailure;
        }
        plans.put(command.planningCommand(), command.plan());
    }

    public void put(
            ZScoreTradePlanningCommand command,
            ZScoreTradePlan plan
    ) {
        plans.put(command, plan);
    }

    public void failSaveWith(RuntimeException failure) {
        this.saveFailure = failure;
    }

    public void failFindOnAttempt(int findAttempt, RuntimeException failure) {
        if (findAttempt < 1) {
            throw new IllegalArgumentException("findAttempt must be greater than zero");
        }
        this.failingFindAttempt = findAttempt;
        this.findFailure = failure;
    }

    public void failSaveAfterConcurrentPlan(
            RuntimeException failure,
            ZScoreTradePlanningCommand command,
            ZScoreTradePlan plan
    ) {
        this.saveFailure = failure;
        this.concurrentCommand = command;
        this.concurrentPlan = plan;
    }

    public SaveZScoreTradePlanCommand command() {
        return command;
    }

    public int findCount() {
        return findCount;
    }

    public int saveCount() {
        return saveCount;
    }

    private static boolean sameRequest(
            ZScoreTradePlanningCommand left,
            ZScoreTradePlanningCommand right
    ) {
        return left.evaluationCommand().equals(right.evaluationCommand())
                && left.purchaseBudget().compareTo(right.purchaseBudget()) == 0;
    }
}
