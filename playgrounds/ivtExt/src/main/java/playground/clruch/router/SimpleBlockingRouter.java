package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * class was generated to unify the code snippets below
 */
// VrpPathWithTravelData newSubPath;
// {
//    ParallelLeastCostPathCalculator router = appender.router;
//    LeastCostPathFuture drivepath = router.calcLeastCostPath( //
//            divLink.getToNode(), destLinks[2].getFromNode(), startTime, null, null);
//    while (!drivepath.isDone()) {
//        try {
//            Thread.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//    newSubPath = VrpPaths.createPath(divLink, destLinks[2], startTime, drivepath.get(), appender.travelTime);
// }
// add the drive task
// Link[] routePoints =new Link[] {stayTask.getLink(),destLinks[1]};
// VrpPathWithTravelData routePoints;
// {
// ParallelLeastCostPathCalculator router = appender.router;
// LeastCostPathFuture drivepath = router.calcLeastCostPath( //
//         stayTask.getLink().getToNode(), destLinks[1].getFromNode(), now, null,
// null);
// while (!drivepath.isDone()) {
// try {
// Thread.sleep(5);
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// }
// routePoints = VrpPaths.createPath(stayTask.getLink(), destLinks[1], now, drivepath.get(), appender.travelTime);
// }
public class SimpleBlockingRouter {
    private final ParallelLeastCostPathCalculator router;
    private final TravelTime travelTime;

    public SimpleBlockingRouter(ParallelLeastCostPathCalculator router, TravelTime travelTime) {
        this.router = router;
        this.travelTime = travelTime;
    }

    /**
     * 
     * @param divLink can be stayTask.getLink() from stay task, or linkTimePair.link from driving task diversion point
     * @param destLink destination link
     * @param startTime can be now when switching from stay task, or linkTimePair.time from driving task diversion point
     * @return
     */
    public VrpPathWithTravelData getRoute(Link divLink, Link destLink, double startTime) {
        LeastCostPathFuture drivepath = router.calcLeastCostPath( //
                divLink.getToNode(), destLink.getFromNode(), startTime, null, null);
        while (!drivepath.isDone())
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        return VrpPaths.createPath(divLink, destLink, startTime, drivepath.get(), travelTime);
    }

}
