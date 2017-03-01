package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;

public abstract class AbstractVehicleDestMatcher {
    public final Map<VehicleLinkPair, Link> match(
            Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        if (vehicleLinkPairs.isEmpty())
            return Collections.emptyMap();
        return protected_match(vehicleLinkPairs, links);
    }

    protected abstract Map<VehicleLinkPair, Link> protected_match(
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            List<Link> links //
    );

}
