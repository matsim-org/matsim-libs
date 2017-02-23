package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.HungarianAlgorithm;

import java.util.*;

/**
 * array matching with Euclidean distance as criteria
 * 
 * TODO suitable for n < ?
 */
public class HungarBiPartVehicleDestMatcher extends AbstractVehicleDestMatcher {
    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {

        // since Collection::iterator does not make guarantees about the order
        List<VehicleLinkPair> ordered_vehicleLinkPairs = new ArrayList<>(vehicleLinkPairs);
        // ensure that the number of vehicles is the same as the number of
        GlobalAssert.that(ordered_vehicleLinkPairs.size() == links.size());

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        final int n = ordered_vehicleLinkPairs.size();
        GlobalAssert.that(0 < n);
        
        final double distancematrix[][] = new double[n][n];

        int i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            Coord vehCoord = vehicleLinkPair.linkTimePair.link.getFromNode().getCoord();
            int j = -1;
            for (Link dest : links) {
                ++j;
                Coord destCoord = dest.getCoord();
                distancematrix[i][j] = Math.hypot( //
                        vehCoord.getX() - destCoord.getX(), //
                        vehCoord.getY() - destCoord.getY());
            }
        }

        HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(distancematrix);

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int matchinghungarianAlgorithm[] = hungarianAlgorithm.execute(); // O(n^3)

        // do the assignment according to the Hungarian algorithm
        Map<VehicleLinkPair, Link> map = new HashMap<>();
        i = -1;
        for (VehicleLinkPair vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            map.put(vehicleLinkPair, (Link) links.get(matchinghungarianAlgorithm[i]));
        }
        return map;


    }
}
