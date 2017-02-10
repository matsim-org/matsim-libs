package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.VehicleLinkPair;

/**
 * array matching without meaningful criteria
 */
public class NativeVehicleDestMatcher extends AbstractVehicleDestMatcher {

    @Override
    public Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, Collection<Link> links) {
        Map<VehicleLinkPair, Link> map = new HashMap<>();
        Iterator<Link> iterator = links.iterator();
        vehicleLinkPairs.stream().forEach(vehicleLinkPair -> map.put(vehicleLinkPair, iterator.next()));
        return map;
    }

}
