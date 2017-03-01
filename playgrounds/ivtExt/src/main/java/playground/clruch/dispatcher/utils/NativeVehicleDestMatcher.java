package playground.clruch.dispatcher.utils;

import java.util.*;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;

/**
 * array matching without meaningful criteria
 */
public class NativeVehicleDestMatcher extends AbstractVehicleDestMatcher {
    // TODO: at a later point List<Link> links might be replaced with a more generic argument such as the request itself (e.g. to allow long-waiting customers to be preferred)
    // TODO: the preferred implementation would be using templates.
    @Override
    public Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        int n = vehicleLinkPairs.size();
        int m = links.size();
        Map<VehicleLinkPair, Link> map = new HashMap<>();

        // assign the links to vehicles in native order
        Iterator<Link> iterator = links.iterator();
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            if (iterator.hasNext()) {
                map.put(vehicleLinkPair, iterator.next());
            }
            // TODO remove this after debugging:
            /*
            else {
                map.put(vehicleLinkPair, (Link) vehicleLinkPair.getDivertableLocation());
            }
            */
        }

        return map;

    }
}
