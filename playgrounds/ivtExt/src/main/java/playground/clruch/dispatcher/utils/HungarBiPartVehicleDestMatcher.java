package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.GlobalAssert;

import java.util.*;

/**
 * array matching without meaningful criteria
 */


public class HungarBiPartVehicleDestMatcher extends AbstractVehicleDestMatcher {
    @Override
    public Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {


        // ensure that the number of vehicles is the same as the number of
        GlobalAssert.that(vehicleLinkPairs.size() == links.size());

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        int n = vehicleLinkPairs.size();
        double distancematrix[][] = new double[n][n];

        int i = -1;
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            i = i + 1;
            Coord vehCoord = vehicleLinkPair.linkTimePair.link.getFromNode().getCoord();
            int j = -1;
            for (Link dest : links) {
                j = j + 1;
                Coord destCoord = dest.getCoord();
                distancematrix[i][j] = Math.hypot((vehCoord.getX() - destCoord.getX()),(vehCoord.getY() - destCoord.getY()));
            }
        }

        HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(distancematrix);

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int matchinghungarianAlgorithm[] = hungarianAlgorithm.execute();



        // do the assignment according to the Hungarian algorithm
        Map<VehicleLinkPair, Link> map = new HashMap<>();
        i = -1;
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            i = i + 1;
            map.put(vehicleLinkPair, (Link) links.get(matchinghungarianAlgorithm[i]));

        }
        return map;


    }
}
