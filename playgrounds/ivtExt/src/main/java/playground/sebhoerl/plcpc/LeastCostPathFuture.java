package playground.sebhoerl.plcpc;

import org.matsim.core.router.util.LeastCostPathCalculator;

public class LeastCostPathFuture {
    final private ParallelLeastCostPathCalculatorTask task;

    public LeastCostPathFuture(ParallelLeastCostPathCalculatorTask task) {
        this.task = task;
    }

    public boolean isDone() {
        return task.result != null;
    }

    public LeastCostPathCalculator.Path get() {
        if (!isDone()) {
            throw new IllegalStateException();
        }

        return task.result;
    }
}
