package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;

/**
 * the purpose of the container is to store an initiated path computation represented by
 * {@link LeastCostPathFuture}   
 * and provide the result 
 * {@link VrpPathWithTravelData}
 * at a later point in time.
 */
public class FuturePathContainer {
    public final Link startLink;
    public final Link destLink;
    public final double startTime;
    public final LeastCostPathFuture leastCostPathFuture;

    private final TravelTime travelTime; // reference for convenience
    private VrpPathWithTravelData vrpPathWithTravelData = null; // <- always private!

    FuturePathContainer(Link startLink, Link destLink, double startTime, LeastCostPathFuture leastCostPathFuture, TravelTime travelTime) {
        this.startLink = startLink;
        this.destLink = destLink;
        this.startTime = startTime;
        this.leastCostPathFuture = leastCostPathFuture;
        this.travelTime = travelTime;
    }

    public final VrpPathWithTravelData getVrpPathWithTravelData() {
        if (vrpPathWithTravelData == null)
            vrpPathWithTravelData = SimpleBlockingRouter.getRouteBlocking( //
                    startLink, destLink, startTime, leastCostPathFuture, travelTime);
        return vrpPathWithTravelData;
    }
}
