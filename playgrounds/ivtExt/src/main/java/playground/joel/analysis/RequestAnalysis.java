/**
 * 
 */
package playground.joel.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import org.matsim.vehicles.Vehicle;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.router.FuturePathFactory;
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
    public static double calcavDistance(Network network, ArrayList<RequestObj> requests) {
        if (requests.size() == 0)
            return 0.0;

        Tensor distances = Tensors.empty();

        LeastCostPathCalculator dijkstra = prepDijkstra(network); // dijkstra approach

        for (RequestObj rObj : requests) {
            // double dist = actualDistance(rObj.fromLink.getCoord(), rObj.toLink.getCoord()); // euclidean approach
            double dist = actualDistance(dijkstra, rObj.fromLink.getFromNode(), rObj.toLink.getToNode()); // dijkstra approach
            distances.append(RealScalar.of(dist));
        }

        RealScalar mean = (RealScalar) Mean.of(distances);
        return mean.number().doubleValue();

    }

    /**
     *
     * @param tData
     * @param virtualNetwork
     * @param dt
     * @param time
     * @return earth mover's distance (EMD) for this set of requests
     */
    public static double calcEMD(TravelData tData, VirtualNetwork virtualNetwork, int dt, int time) {
        Tensor alphaij = tData.getAlphaijforTime(time);
        Tensor dist = Tensors.matrix((i, j) -> vLinkDistance(i,j, virtualNetwork), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        RealScalar total = (RealScalar) Total.of(Total.of(alphaij.pmul(dist)));
        return total.number().doubleValue();
    }

    /**
     * 
     * @param requests
     * @return average AVspeed for this set of requests
     */
    public static double calcAVSpeed(Network network, ArrayList<RequestObj> requests) {
        // TODO implement a more accurate version where the returned speed represents a good but low estimation of the average
        // speed of AVs in that slice.
        return network.getLinks().values().stream().mapToDouble(Link::getFreespeed).min().getAsDouble(); // smallest freespeed
    }
    
    private static RealScalar vLinkDistance(int i, int j, VirtualNetwork virtualNetwork){
        
        // TODO make more realistic, step 1: include factor from Euclidean to Network distance, step 2: full shortest path distance (then also add for LP)
        VirtualNode vi = virtualNetwork.getVirtualNode(i);
        VirtualNode vj = virtualNetwork.getVirtualNode(j);
        // TODO: find fitting nodes for dijkstra
        return RealScalar.of(actualDistance(vi.getCoord(), vj.getCoord()));
        
    }

    // TODO make more realistic, step 1: include factor from Euclidean to Network distance, step 2: full shortest path distance (then also add for LP)
    public static double actualDistance(Coord from, Coord to) {
        double dist = CoordUtils.calcEuclideanDistance(from, to);
        return dist;
    }

    public static double actualDistance(LeastCostPathCalculator dijkstra, Node from, Node to) {
        double dist = 0;
        // TODO: need start time? k*dt
        for (Link link : dijkstra.calcLeastCostPath(from, to, 0.0, null, null).links) {
            dist += link.getLength();
        }
        return dist;
    }

    public static LeastCostPathCalculator prepDijkstra(Network network) {
        PreProcessDijkstra preProcessData = new PreProcessDijkstra();
        preProcessData.run(network);

        TravelDisutility travelMinCost =  new TravelDisutility() {

            @Override
            public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                return getLinkMinimumTravelDisutility(link);
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return link.getLength()/link.getFreespeed();
            }
        };

        TravelTime travelTime = new TravelTime() {
            public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength()/link.getFreespeed();
            }
        };

        return (new FastDijkstraFactory(preProcessData)).createPathCalculator(network, travelMinCost, travelTime);
    }

}
