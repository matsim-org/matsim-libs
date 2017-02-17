package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.VehicleLinkPair;

public abstract class AbstractVehicleDestMatcher {
    public final Map<VehicleLinkPair, Link> match( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<Link> links) {
        if (vehicleLinkPairs.size() != links.size())
            throw new RuntimeException("set of vehicles and links inconsistent size");
        return protected_match(vehicleLinkPairs, links);
    }

    public abstract Map<VehicleLinkPair, Link> protected_match( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<Link> links //
    );

}
