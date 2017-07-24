// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehicleDestMatcher {

    /**
     * @param vehicleLinkPairs
     * @param links collection may be modified inside the matcher
     * @return
     */
    public final Map<VehicleLinkPair, AVRequest> matchAVRequest( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<AVRequest> avRequests) {
        if (vehicleLinkPairs.isEmpty() || avRequests.isEmpty())
            return Collections.emptyMap();
        return protected_matchAVRequest(vehicleLinkPairs, avRequests);
    }

    protected abstract Map<VehicleLinkPair, AVRequest> protected_matchAVRequest( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<AVRequest> links //
    );
    
    
    public final Map<VehicleLinkPair, Link> matchLink( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<Link> destinations) {
        if (vehicleLinkPairs.isEmpty() || destinations.isEmpty())
            return Collections.emptyMap();
        return protected_matchLink(vehicleLinkPairs, destinations);
    }

    protected abstract Map<VehicleLinkPair, Link> protected_matchLink( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<Link> links //
    );    
    
    

}
