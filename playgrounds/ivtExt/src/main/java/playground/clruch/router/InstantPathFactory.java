// code by jph
package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class InstantPathFactory {
    final ParallelLeastCostPathCalculator parallelLeastCostPathCalculator;
    final TravelTime travelTime;

    public InstantPathFactory( //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            TravelTime travelTime) {
        this.parallelLeastCostPathCalculator = parallelLeastCostPathCalculator;
        this.travelTime = travelTime;
    }

    public VrpPathWithTravelData getVrpPathWithTravelData(Link startLink, Link destLink, double startTime) {
        LeastCostPathFuture leastCostPathFuture = parallelLeastCostPathCalculator.calcLeastCostPath( // <- non-blocking call
                startLink.getToNode(), destLink.getFromNode(), startTime, null, null);
        return new FuturePathContainer(startLink, destLink, startTime, leastCostPathFuture, travelTime).getVrpPathWithTravelData();
    }
}
