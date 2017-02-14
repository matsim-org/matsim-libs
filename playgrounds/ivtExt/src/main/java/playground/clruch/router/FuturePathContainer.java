package playground.clruch.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.plcpc.LeastCostPathFuture;

public class FuturePathContainer {
    public final Link divLink; // TODO rename
    public final Link destLink;
    public final double startTime;
    public final LeastCostPathFuture leastCostPathFuture;

    private final TravelTime travelTime; // reference for convenience
    private VrpPathWithTravelData vrpPathWithTravelData = null; // <- always private!

    FuturePathContainer(Link divLink, Link destLink, double startTime, LeastCostPathFuture leastCostPathFuture, TravelTime travelTime) {
        this.destLink = destLink;
        this.divLink = divLink;
        this.startTime = startTime;
        this.leastCostPathFuture = leastCostPathFuture;
        this.travelTime = travelTime;
    }

    public final VrpPathWithTravelData getVrpPathWithTravelData() {
        if (vrpPathWithTravelData == null)
            vrpPathWithTravelData = SimpleBlockingRouter.getRouteBlocking( //
                    divLink, destLink, startTime, leastCostPathFuture, travelTime);
        return vrpPathWithTravelData;
    }

    // VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
    // avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

    // VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
    // vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

    // VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
    // vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

}
