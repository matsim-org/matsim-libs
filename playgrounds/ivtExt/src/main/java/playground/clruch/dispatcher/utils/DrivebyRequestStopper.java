package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * this class is used for testing old functionality
 * the class is superseded by DivertIfCurrentDestinationEmpty etc.
 */
public class DrivebyRequestStopper {

    final BiConsumer<VehicleLinkPair, Link> biConsumer;

    public DrivebyRequestStopper(BiConsumer<VehicleLinkPair, Link> biConsumer) {
        this.biConsumer = biConsumer;
    }

    /**
     * see if any car is driving by a request. if so, cancel driving path, and make link new goal.
     * Then stay there to be matched ASAP or become available for rerouting.
     * 
     * @param requests
     * @param divertableVehicles
     * @return
     */
    public int realize(Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> divertableVehicles) {
        int num_abortTrip = 0;
        for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
            Link link = vehicleLinkPair.getDivertableLocation();
            if (requests.containsKey(link)) {
                List<AVRequest> requestList = requests.get(link);
                if (!requestList.isEmpty()) {
                    requestList.remove(0);
                    biConsumer.accept(vehicleLinkPair, link);
                    ++num_abortTrip;
                }
            }
        }
        return num_abortTrip;
    }

}
