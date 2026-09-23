package org.matsim.contrib.pseudosimulation.distributed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

final class SlaveIterationOrchestrator {
    private final Operations operations;
    private final LongSupplier clock;
    private List<Long> iterationTimes = new ArrayList<>();
    private long lastIterationStartTime;
    private int iterationsPerCycle;
    private int numberOfIterations = -1;
    private int currentIteration;
    private double totalIterationTime;
    private boolean initialRouting;
    private boolean somethingWentWrong;

    SlaveIterationOrchestrator(Operations operations) {
        this(operations, System::currentTimeMillis);
    }

    SlaveIterationOrchestrator(Operations operations, LongSupplier clock) {
        this.operations = operations;
        this.clock = clock;
        lastIterationStartTime = clock.getAsLong();
    }

    void configure(boolean routeInitially, int cycleLength) {
        initialRouting = routeInitially;
        iterationsPerCycle = cycleLength;
    }

    void startup() {
        communicate();
    }

    void communicateNow() {
        communicate();
    }

    void iterationStarts(int iteration) {
        if (numberOfIterations >= 0 || initialRouting) {
            iterationTimes.add(clock.getAsLong() - lastIterationStartTime);
        }
        if (initialRouting || numberOfIterations > 0 && numberOfIterations % iterationsPerCycle == 0) {
            totalIterationTime = sumIterationTimes();
            communicate();
            if (somethingWentWrong) {
                operations.halt();
            }
            initialRouting = false;
        }
        currentIteration = iteration;
        lastIterationStartTime = clock.getAsLong();
        operations.activateTravelTime();
        operations.initializePlanCatcher();
        numberOfIterations++;
    }

    void shutdown() {
        totalIterationTime = sumIterationTimes();
        communicate();
        communicate();
    }

    void resetIterationTimes() {
        iterationTimes = new ArrayList<>();
    }

    int currentIteration() {
        return currentIteration;
    }

    double totalIterationTime() {
        return totalIterationTime;
    }

    double sumIterationTimes() {
        double sum = 0;
        for (long time : iterationTimes) {
            sum += time;
        }
        return sum;
    }

    private void communicate() {
        if (!operations.communicate()) {
            somethingWentWrong = true;
        }
    }

    interface Operations {
        boolean communicate();
        void halt();
        void activateTravelTime();
        void initializePlanCatcher();
    }
}
