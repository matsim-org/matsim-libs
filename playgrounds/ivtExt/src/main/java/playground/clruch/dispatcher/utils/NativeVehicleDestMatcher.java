package playground.clruch.dispatcher.utils;

import java.util.*;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;

/**
 * array matching without meaningful criteria
 */
public class NativeVehicleDestMatcher extends AbstractVehicleDestMatcher {

    @Override
    public Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        Map<VehicleLinkPair, Link> map = new HashMap<>();
        Iterator<Link> iterator = links.iterator();
        vehicleLinkPairs.stream().forEach(vehicleLinkPair -> map.put(vehicleLinkPair, iterator.next()));
        return map;
    }

}
