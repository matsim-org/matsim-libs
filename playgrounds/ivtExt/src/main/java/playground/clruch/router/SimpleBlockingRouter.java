package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * class obtains the path that connects two given nodes
 */
@Deprecated
class SimpleBlockingRouter {
    private final ParallelLeastCostPathCalculator router;
    private final TravelTime travelTime;

    SimpleBlockingRouter(ParallelLeastCostPathCalculator router, TravelTime travelTime) {
        this.router = router;
        this.travelTime = travelTime;
    }

    private LeastCostPathFuture getRouteFuture(Link divLink, Link destLink, double startTime) {
        return router.calcLeastCostPath( // <- non-blocking call
                divLink.getToNode(), destLink.getFromNode(), startTime, null, null);
    }

    /**
     * 
     * @param divLink
     *            can be stayTask.getLink() from stay task, or linkTimePair.link from driving task diversion point
     * @param destLink
     *            destination link
     * @param startTime
     *            can be now when switching from stay task, or linkTimePair.time from driving task diversion point
     * @return
     */
    VrpPathWithTravelData getRoute(Link divLink, Link destLink, double startTime) {
        LeastCostPathFuture leastCostPathFuture = getRouteFuture(divLink, destLink, startTime);

        return FuturePathContainer.getRouteBlocking(divLink, destLink, startTime, leastCostPathFuture, travelTime);
    }

}
