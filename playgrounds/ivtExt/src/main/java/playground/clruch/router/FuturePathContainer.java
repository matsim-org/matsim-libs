// code by jph
package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.plcpc.LeastCostPathFuture;

/**
 * the purpose of the container is to store an initiated path computation represented by
 * {@link LeastCostPathFuture}   
 * and provide the result 
 * {@link VrpPathWithTravelData}
 * at a later point in time.
 */
public final class FuturePathContainer {
    private final Link startLink;
    private final Link destLink;
    private final double startTime;
    private final LeastCostPathFuture leastCostPathFuture;

    private final TravelTime travelTime; // reference for convenience
    private VrpPathWithTravelData vrpPathWithTravelData = null; // <- always private!

    FuturePathContainer(Link startLink, Link destLink, double startTime, LeastCostPathFuture leastCostPathFuture, TravelTime travelTime) {
        this.startLink = startLink;
        this.destLink = destLink;
        this.startTime = startTime;
        this.leastCostPathFuture = leastCostPathFuture;
        this.travelTime = travelTime;
    }
    
    public double getStartTime() {
        return startTime;
    }

    public final VrpPathWithTravelData getVrpPathWithTravelData() {
        if (vrpPathWithTravelData == null)
            vrpPathWithTravelData = getRouteBlocking( //
                    startLink, destLink, startTime, leastCostPathFuture, travelTime);
        return vrpPathWithTravelData;
    }

    private static VrpPathWithTravelData getRouteBlocking(Link startLink, Link destLink, double startTime, LeastCostPathFuture leastCostPathFuture, TravelTime travelTime) {
        try {
            Thread.sleep(0, 100000);
            while (!leastCostPathFuture.isDone())
                Thread.sleep(0, 100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.createPath(startLink, destLink, startTime, leastCostPathFuture.get(), travelTime);
        GlobalAssert.that(VrpPathUtils.isConsistent(vrpPathWithTravelData));
        return vrpPathWithTravelData;
    }
}
