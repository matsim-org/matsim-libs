// code by clruch
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.GlobalBipartiteMatchingDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;

import java.util.*;

/**
 * this call removes the matched links from links
 * 
 * matcher is used in {@link GlobalBipartiteMatchingDispatcher}
 * 
 * Created by Claudio on 3/10/2017.
 */
public class ImmobilizeVehicleDestMatcher extends AbstractVehicleDestMatcher {

    // TODO the current implementation is O(n*m), this could be improved
    /**
     * for every request, it immobilizes one vehicle which is on the same link as the request
     */
    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        // create map with the pairs
        Map<VehicleLinkPair, Link> returnmap = new HashMap<>();
        int i = 0;
        while(i<links.size()){
            Link link = links.get(i);
            for (VehicleLinkPair vehicleLinkpair : vehicleLinkPairs) {
                if (link.equals(vehicleLinkpair.getDivertableLocation())) {
                    returnmap.put(vehicleLinkpair, link);
                    links.remove(i);
                    break;
                }
            }
            ++i;
        }
            


        return returnmap;
    }
}
