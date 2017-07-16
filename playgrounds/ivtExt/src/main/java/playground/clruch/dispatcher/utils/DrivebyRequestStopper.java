// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * this class is used for testing old functionality the class is superseded by DivertIfCurrentDestinationEmpty etc.
 */
public enum DrivebyRequestStopper {
    ;

    public static int stopDrivingBy(Map<Link, List<AVRequest>> requestLocs, Collection<VehicleLinkPair> divertableVehicles,
            BiConsumer<AVVehicle, AVRequest> biConsumer) {
        int numDriveByPickup = 0;

        for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
            Link link = vehicleLinkPair.getDivertableLocation();
            if (requestLocs.containsKey(link)) {
                List<AVRequest> requestList = requestLocs.get(link);
                if (!requestList.isEmpty()) {
                    AVRequest request = requestList.get(0);
                    biConsumer.accept(vehicleLinkPair.avVehicle, request);
                    ++numDriveByPickup;
                }
            }
        }
        return numDriveByPickup;
    }
}

// public class DrivebyRequestStopper {
//
// final BiConsumer<AVVehicle, Link> biConsumer;
//
// public DrivebyRequestStopper(BiConsumer<AVVehicle, Link> biConsumer) {
// this.biConsumer = biConsumer;
// }
//
// /**
// * see if any car is driving by a request. if so, cancel driving path, and make link new goal. Then stay there to be matched ASAP or become
// * available for rerouting.
// *
// * @param requests
// * @param divertableVehicles
// * @return
// */
// public int realize(Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> divertableVehicles) {
// int num_abortTrip = 0;
// for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
// Link link = vehicleLinkPair.getDivertableLocation();
// if (requests.containsKey(link)) {
// List<AVRequest> requestList = requests.get(link);
// if (!requestList.isEmpty()) {
// requestList.remove(0);
// biConsumer.accept(vehicleLinkPair.avVehicle, link);
// ++num_abortTrip;
// }
// }
// }
// return num_abortTrip;
// }
//
// }
