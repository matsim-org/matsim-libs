package playground.clruch.dispatcher.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.HungarianAlgorithm;

/**
 * array matching with Euclidean distance as criteria
 *
 */
public class HungarBiPartVehicleDestMatcher extends AbstractVehicleDestMatcher {

    private static Coord getLinkCoord(Link link) {
        return link.getFromNode().getCoord();
    }

    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {

        // since Collection::iterator does not make guarantees about the order we store the pairs in a list
        final List<VehicleLinkPair> ordered_vehicleLinkPairs = new ArrayList<>(vehicleLinkPairs);

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        final int n = ordered_vehicleLinkPairs.size(); // workers
        final int m = links.size(); // jobs

        final double distancematrix[][] = new double[n][m];

        int i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            final Coord vehCoord = getLinkCoord(vehicleLinkPair.linkTimePair.link); // ...link.getFromNode().getCoord(); // divertible location
            int j = -1;
            for (Link dest : links) {
                ++j;
                Coord destCoord = getLinkCoord(dest); // dest.getFromNode().getCoord(); // also use fromNode as reference
                distancematrix[i][j] = Math.hypot( //
                        vehCoord.getX() - destCoord.getX(), //
                        vehCoord.getY() - destCoord.getY());
            }
        }

        HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(distancematrix);

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int[] matchinghungarianAlgorithm = hungarianAlgorithm.execute(); // O(n^3)

        // do the assignment according to the Hungarian algorithm (only for the matched elements, otherwise keep current drive destination)
        final Map<VehicleLinkPair, Link> map = new HashMap<>();
        i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            if (0 <= matchinghungarianAlgorithm[i]) 
                map.put(vehicleLinkPair, links.get(matchinghungarianAlgorithm[i]));            
        }
        return map;
    }
}
