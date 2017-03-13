package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * 
 */
@ Deprecated
public class DrivebyRequestStopper {

    final BiFunction<VehicleLinkPair, Link, Void> biFunction;

    public DrivebyRequestStopper(BiFunction<VehicleLinkPair, Link, Void> biFunction) {
        this.biFunction = biFunction;
    }

    /**
     * see if any car is driving by a request. if so, cancel driving path, and make link new goal.
     * Then stay there to be matched ASAP or become available for rerouting.
     * 
     * @param requests
     * @param divertableVehicles
     * @return
     */
    public Collection<VehicleLinkPair> realize(Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> divertableVehicles) {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
            final Link link = vehicleLinkPair.getDivertableLocation();
            if (requests.containsKey(link)) {
                List<AVRequest> requestList = requests.get(link);
                if (!requestList.isEmpty()) {
                    final AVRequest avRequest = requestList.get(0);
                    final Link requestLink = avRequest.getFromLink();// .getId();
                    GlobalAssert.that(link == requestLink); // <- equals by reference
                    if (!vehicleLinkPair.isDivertableLocationAlsoDriveTaskDestination()) {
                        System.out.println(" SHORT " + vehicleLinkPair.getDivertableLocation().getId());
                        System.out.println("  \\-   " + vehicleLinkPair.getCurrentDriveDestination().getId());
                        // System.out.println("TIME FOR DIVERSION " + vehicleLinkPair.linkTimePair.time);
                        // System.out.println("requests.contains link " + link.getId().toString());
                        // System.out.println("requestList.size() " //
                        // + requestList.size() + " " //
                        // + vehicleLinkPair.avVehicle.getOperator().getId().toString() + " " + link);
                        collection.add(vehicleLinkPair);
                        biFunction.apply(vehicleLinkPair, link);
                    }
                    requestList.remove(0);
                }
            }
        }
        return collection;
    }

}
