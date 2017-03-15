package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;
import playground.clruch.dispatcher.core.VehicleLinkPair;

import java.util.*;

/**
 * this call removes the matched links from links
 * 
 * Created by Claudio on 3/10/2017.
 */
public class ImmobilizeVehicleDestMatcher extends AbstractVehicleDestMatcher {
 
    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        Map<VehicleLinkPair, Link> returnmap = new HashMap<>();
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            int i = 0;
            while (i < links.size()) {
                Link link = links.get(i);
                if (link.equals(vehicleLinkPair.getCurrentDriveDestination())) {
                    returnmap.put(vehicleLinkPair, link);
                    links.remove(i);
                    break;
                } else {
                    i = i + 1;
                }
            }
        }
        return returnmap;
    }
}
