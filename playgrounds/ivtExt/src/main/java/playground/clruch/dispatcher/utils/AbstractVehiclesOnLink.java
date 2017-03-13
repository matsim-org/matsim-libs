package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehiclesOnLink extends AbstractVehicleRequestLinkMatcher {

    // System.out.println(" SHORT " + vehicleLinkPair.getDivertableLocation().getId());
    // System.out.println(" \\- " + vehicleLinkPair.getCurrentDriveDestination().getId());

    @Override
    public final Map<VehicleLinkPair, Link> match( //
            Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> vehicles) {
        final Map<VehicleLinkPair, Link> map = new HashMap<>();
        for (VehicleLinkPair vehicleLinkPair : vehicles) {
            final Link link = vehicleLinkPair.getDivertableLocation();
            if (requests.containsKey(link)) {
                final List<AVRequest> requestList = requests.get(link); // by reference
                if (!requestList.isEmpty()) {
                    final AVRequest avRequest = requestList.get(0);
                    GlobalAssert.that(link == avRequest.getFromLink()); // <- equals by reference
                    if (vehicleLinkPair.isDivertableLocationAlsoDriveTaskDestination()) {
                        map.put(vehicleLinkPair, link);
                    }
                    requestList.remove(0); // <- here so that requests are matched to
                }
            }
        }
        return map;
    }

    public abstract boolean shouldStop(VehicleLinkPair vehicleLinkPair);

}
