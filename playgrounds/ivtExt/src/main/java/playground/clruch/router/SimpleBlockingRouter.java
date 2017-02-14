package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.VrpPathUtils;
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
    
    public LeastCostPathFuture getRouteFuture(Link divLink, Link destLink, double startTime) {
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
    public VrpPathWithTravelData getRoute(Link divLink, Link destLink, double startTime) {
        // TODO call function above
        LeastCostPathFuture leastCostPathFuture = router.calcLeastCostPath( // <- non-blocking call
                divLink.getToNode(), destLink.getFromNode(), startTime, null, null);
        
        // TODO call static method below instead
        try {
            Thread.sleep(1); // TODO sleep less
            while (!leastCostPathFuture.isDone())
                Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.createPath(divLink, destLink, startTime, leastCostPathFuture.get(), travelTime);
        
        VrpPathUtils.assertIsConsistent(vrpPathWithTravelData);
        return vrpPathWithTravelData;
    }
    
    public static VrpPathWithTravelData getRouteBlocking(Link divLink, Link destLink, double startTime, LeastCostPathFuture leastCostPathFuture, TravelTime travelTime) {
        try {
            Thread.sleep(1); // TODO sleep less
            while (!leastCostPathFuture.isDone())
                Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.createPath(divLink, destLink, startTime, leastCostPathFuture.get(), travelTime);
        
        VrpPathUtils.assertIsConsistent(vrpPathWithTravelData);
        return vrpPathWithTravelData;
    }

}
