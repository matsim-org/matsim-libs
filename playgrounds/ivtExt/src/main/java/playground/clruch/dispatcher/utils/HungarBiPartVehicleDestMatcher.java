// code by clruch
package playground.clruch.dispatcher.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.math.HungarianAlgorithm;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * array matching with Euclidean distance as criteria
 */
// TODO in here there is some dupliate code that could be removed. 
public class HungarBiPartVehicleDestMatcher extends AbstractVehicleDestMatcher {

    private final DistanceFunction distancer;
    
    public HungarBiPartVehicleDestMatcher(DistanceFunction distancer){
        this.distancer = distancer;
        
    }
    

    @Override
    protected Map<RoboTaxi, AVRequest> protected_matchAVRequest(Collection<RoboTaxi> robotaxis, Collection<AVRequest> requests) {

        // since Collection::iterator does not make guarantees about the order we store the pairs in a list
        final List<RoboTaxi> ordered_vehicleLinkPairs = new ArrayList<>(robotaxis);
        final List<AVRequest> ordered_requests = new ArrayList<>(requests);

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        final int n = ordered_vehicleLinkPairs.size(); // workers
        final int m = ordered_requests.size(); // jobs

        final double[][] distancematrix = new double[n][m];

        int i = -1;
        for (RoboTaxi vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            int j = -1;
            for (AVRequest avRequest : ordered_requests) {
                Link dest = avRequest.getFromLink();
                distancematrix[i][++j] = distancer.getDistance(vehicleLinkPair, dest);
            }
        }

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int[] matchinghungarianAlgorithm = new HungarianAlgorithm(distancematrix).executeClruch(); // O(n^3)

        // do the assignment according to the Hungarian algorithm (only for the matched elements, otherwise keep current drive destination)
        final Map<RoboTaxi, AVRequest> map = new HashMap<>();
        i = -1;
        for (RoboTaxi vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            if (0 <= matchinghungarianAlgorithm[i]) {
                map.put(vehicleLinkPair, ordered_requests.get(matchinghungarianAlgorithm[i]));
            }
        }

        GlobalAssert.that(map.size() == Math.min(n, m));
        return map;
    }

    @Override
    protected Map<RoboTaxi, Link> protected_matchLink(Collection<RoboTaxi> vehicleLinkPairs, Collection<Link> links) {

        // since Collection::iterator does not make guarantees about the order we store the pairs in a list
        final List<RoboTaxi> ordered_vehicleLinkPairs = new ArrayList<>(vehicleLinkPairs);
        final List<Link> ordered_Links = new ArrayList<>(links);

        // cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j
        final int n = ordered_vehicleLinkPairs.size(); // workers
        final int m = ordered_Links.size(); // jobs

        final double[][] distancematrix = new double[n][m];

        int i = -1;
        for (RoboTaxi vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            int j = -1;
            for (Link link : ordered_Links) {
                distancematrix[i][++j] = distancer.getDistance(vehicleLinkPair, link);
            }
        }

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int[] matchinghungarianAlgorithm = new HungarianAlgorithm(distancematrix).execute(); // O(n^3)

        // do the assignment according to the Hungarian algorithm (only for the matched elements, otherwise keep current drive destination)
        final Map<RoboTaxi, Link> map = new HashMap<>();
        i = -1;
        for (RoboTaxi vehicleLinkPair : ordered_vehicleLinkPairs) {
            ++i;
            if (0 <= matchinghungarianAlgorithm[i]) {
                map.put(vehicleLinkPair, ordered_Links.get(matchinghungarianAlgorithm[i]));
            }
        }

        GlobalAssert.that(map.size() == Math.min(n, m));
        return map;

    }

}
