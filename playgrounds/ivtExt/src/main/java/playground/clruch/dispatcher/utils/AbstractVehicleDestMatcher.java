// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;

public abstract class AbstractVehicleDestMatcher {

    /**
     * @param vehicleLinkPairs
     * @param links collection may be modified inside the matcher
     * @return
     */
    public final Map<VehicleLinkPair, Link> match( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            List<Link> links) {
        if (vehicleLinkPairs.isEmpty() || links.isEmpty())
            return Collections.emptyMap();
        return protected_match(vehicleLinkPairs, links);
    }

    protected abstract Map<VehicleLinkPair, Link> protected_match( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            List<Link> links //
    );

}
