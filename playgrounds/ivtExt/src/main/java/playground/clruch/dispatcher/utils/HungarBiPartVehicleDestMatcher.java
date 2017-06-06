// code by clruch
package playground.clruch.dispatcher.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.HungarianAlgorithm;

/**
 * array matching with Euclidean distance as criteria
 */
public class HungarBiPartVehicleDestMatcher extends AbstractVehicleDestMatcher {

    private static Coord getLinkCoord(Link link) {
        return link.getFromNode().getCoord();
    }

    private static double getDistance(Link link1, Link link2) {
        final Coord c1 = getLinkCoord(link1);
        final Coord c2 = getLinkCoord(link2);
        return Math.hypot( //
                c1.getX() - c2.getX(), //
                c1.getY() - c2.getY());
    }

    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {

        // since Collection::iterator does not make guarantees about the order we store the pairs in a list
        final List<VehicleLinkPair> ordered_vehicleLinkPairs = new ArrayList<>(vehicleLinkPairs);

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        final int n = ordered_vehicleLinkPairs.size(); // workers
        final int m = links.size(); // jobs

        final double[][] distancematrix = new double[n][m];

        int i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            int j = -1;
            for (Link dest : links)                 
                distancematrix[i][++j] = getDistance(vehicleLinkPair.linkTimePair.link, dest);            
        }

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int[] matchinghungarianAlgorithm = new HungarianAlgorithm(distancematrix).execute(); // O(n^3)

        // do the assignment according to the Hungarian algorithm (only for the matched elements, otherwise keep current drive destination)
        final Map<VehicleLinkPair, Link> map = new HashMap<>();
        i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            if (0 <= matchinghungarianAlgorithm[i])
                map.put(vehicleLinkPair, links.get(matchinghungarianAlgorithm[i]));
        }
        GlobalAssert.that(map.size() == Math.min(n, m));
        return map;
    }
}
