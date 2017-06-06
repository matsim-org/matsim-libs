// code by jph
package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * factory that emits {@link FuturePathContainer}
 */
public class FuturePathFactory {
    private final ParallelLeastCostPathCalculator parallelLeastCostPathCalculator;
    private final TravelTime travelTime;

    public FuturePathFactory( //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            TravelTime travelTime) {
        this.parallelLeastCostPathCalculator = parallelLeastCostPathCalculator;
        this.travelTime = travelTime;
    }

    public FuturePathContainer createFuturePathContainer(Link startLink, Link destLink, double startTime) {
        LeastCostPathFuture leastCostPathFuture = parallelLeastCostPathCalculator.calcLeastCostPath( // <- non-blocking call
                startLink.getToNode(), destLink.getFromNode(), startTime, null, null);
        return new FuturePathContainer(startLink, destLink, startTime, leastCostPathFuture, travelTime);
    }
}
