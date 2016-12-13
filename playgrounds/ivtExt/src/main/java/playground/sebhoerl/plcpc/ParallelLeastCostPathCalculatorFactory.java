package playground.sebhoerl.plcpc;

import org.matsim.core.router.util.LeastCostPathCalculator;

public interface ParallelLeastCostPathCalculatorFactory {
    LeastCostPathCalculator createRouter();
}
