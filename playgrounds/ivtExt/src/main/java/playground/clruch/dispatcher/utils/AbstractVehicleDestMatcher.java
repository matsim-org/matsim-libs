// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehicleDestMatcher {

    /**
     * @param vehicleLinkPairs
     * @param links collection may be modified inside the matcher
     * @return
     */
    public final Map<RoboTaxi, AVRequest> matchAVRequest( //
            Collection<RoboTaxi> vehicleLinkPairs, //
            Collection<AVRequest> avRequests) {
        if (vehicleLinkPairs.isEmpty() || avRequests.isEmpty())
            return Collections.emptyMap();
        return protected_matchAVRequest(vehicleLinkPairs, avRequests);
    }

    protected abstract Map<RoboTaxi, AVRequest> protected_matchAVRequest( //
            Collection<RoboTaxi> vehicleLinkPairs, //
            Collection<AVRequest> links //
    );
    
    
    public final Map<RoboTaxi, Link> matchLink( //
            Collection<RoboTaxi> vehicleLinkPairs, //
            Collection<Link> destinations) {
        if (vehicleLinkPairs.isEmpty() || destinations.isEmpty())
            return Collections.emptyMap();
        return protected_matchLink(vehicleLinkPairs, destinations);
    }

    protected abstract Map<RoboTaxi, Link> protected_matchLink( //
            Collection<RoboTaxi> vehicleLinkPairs, //
            Collection<Link> links //
    );    
    
    

}
