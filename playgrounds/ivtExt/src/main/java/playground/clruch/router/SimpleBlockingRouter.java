package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * class obtains the path that connects two given nodes
 */
public class SimpleBlockingRouter {
    private final ParallelLeastCostPathCalculator router;
    private final TravelTime travelTime;

    public SimpleBlockingRouter(ParallelLeastCostPathCalculator router, TravelTime travelTime) {
        this.router = router;
        this.travelTime = travelTime;
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
    public VrpPathWithTravelData getRoute(Link divLink, Link destLink, double startTime) {
        LeastCostPathFuture drivepath = router.calcLeastCostPath( //
                divLink.getToNode(), destLink.getFromNode(), startTime, null, null);
        try {
            Thread.sleep(1);
            while (!drivepath.isDone())
                Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return VrpPaths.createPath(divLink, destLink, startTime, drivepath.get(), travelTime);
    }

}
