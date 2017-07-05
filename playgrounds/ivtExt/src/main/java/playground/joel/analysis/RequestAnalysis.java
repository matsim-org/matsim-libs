/**
 * 
 */
package playground.joel.analysis;

import java.util.ArrayList;

import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.traveldata.TravelData;

/**
 * @author Claudio Ruch
 *
 */
public enum RequestAnalysis {
    ;

    /**
     * 
     * @param requests
     * @return arrival rate for this set of requests
     */
    public static double calcArrivalRate(ArrayList<RequestObj> requests, double dt) {
        return requests.size() / dt;
    }

    /**
     * 
     * @param requests
     * @return average travel distance for this set of requests
     */
    public static double calcavDistance(ArrayList<RequestObj> requests) {
        if (requests.size() == 0)
            return 0.0;
        // TODO implement real network distances instead of Euclidean distances
        Tensor distances = Tensors.empty();

        for (RequestObj rObj : requests) {

            double dist = CoordUtils.calcEuclideanDistance(rObj.fromLink.getCoord(), rObj.toLink.getCoord());
            distances.append(RealScalar.of(dist));
        }

        RealScalar mean = (RealScalar) Mean.of(distances);
        return mean.number().doubleValue();

    }

    /**
     * 
     * @param requests
     * @return earth mover's distance (EMD) for this set of requests
     */
    public static double calcEMD(TravelData tData, VirtualNetwork virtualNetwork, int dt, int time) {

        Tensor alphaij = tData.getAlphaijforTime(time);
        // TODO replace the 1.0 with distances
        Tensor dist = Tensors.matrix((i, j) -> vLinkDistance(i,j, virtualNetwork), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());

        RealScalar total = (RealScalar) Total.of(Total.of(alphaij.pmul(dist)));

        return total.number().doubleValue();

    }

    /**
     * 
     * @param requests
     * @return average AVspeed for this set of requests
     */
    public static double calcAVSpeed(ArrayList<RequestObj> requests) {
        // TODO implement a more accurate version where the returned speed represents a good but low estimation of the average
        // speed of AVs in that slice.
        return 33.0/3.6;
    }
    
    private static RealScalar vLinkDistance(int i, int j, VirtualNetwork virtualNetwork){
        
        // TODO make more realistic, step 1: include factor from Euclidean to Network distance, step 2: full shortest path distance (then also addd for LP)
        VirtualNode vi = virtualNetwork.getVirtualNode(i);
        VirtualNode vj = virtualNetwork.getVirtualNode(j);
        
        return RealScalar.of(CoordUtils.calcEuclideanDistance(vi.getCoord(), vj.getCoord()));
        
    }

}
