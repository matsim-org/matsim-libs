/**
 * 
 */
package playground.joel.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.traveldata.TravelData;
import playground.joel.helpers.EasyDijkstra;

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
    public static double calcavDistance(LeastCostPathCalculator dijkstra, ArrayList<RequestObj> requests) {
        if (requests.size() == 0)
            return 0.0;

        Tensor distances = Tensors.empty();

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
    public static double calcEMD(TravelData tData, VirtualNetwork virtualNetwork, LeastCostPathCalculator dijkstra, //
                                 int dt, int time) {
        Tensor alphaij = tData.getAlphaijforTime(time);
        AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
        Tensor dist = Tensors.matrix((i, j) -> vLinkDistance(i,j, virtualNetwork, dijkstra, abstractVirtualNodeDest), //
                virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        RealScalar total = (RealScalar) Total.of(Total.of(alphaij.pmul(dist)));
        return total.number().doubleValue();
    }

    // TODO implement a more accurate version where the returned speed represents a good but low estimation of the average
    // speed of AVs in that slice.
    /**
     *
     * @param dijkstra
     * @param requests
     * @return average AVspeed for this set of requests
     */
    public static double calcAVSpeed(LeastCostPathCalculator dijkstra, ArrayList<RequestObj> requests) {
        double dist = 0.0;
        double time = 0.0;
        for (RequestObj rObj : requests) {
            LeastCostPathCalculator.Path path = EasyDijkstra.executeDijkstra(dijkstra, rObj.fromLink.getFromNode(), rObj.toLink.getToNode());
            for (Link link : path.links) {
                dist += link.getLength();
            }
            time += path.travelTime;
        }
        return time == 0.0 ? 0.0 : dist/time;
    }

    public static double calcAVSpeed(Network network) {
        return network.getLinks().values().stream().mapToDouble(Link::getFreespeed).min().getAsDouble(); // smallest freespeed in network
    }
    
    private static RealScalar vLinkDistance(int i, int j, VirtualNetwork virtualNetwork, //
                                           LeastCostPathCalculator dijkstra, //
                                           AbstractVirtualNodeDest abstractVirtualNodeDest) {
        VirtualNode vi = virtualNetwork.getVirtualNode(i);
        VirtualNode vj = virtualNetwork.getVirtualNode(j);

        // return RealScalar.of(actualDistance(vi.getCoord(), vj.getCoord())); // euclidean approach

        Node ni = virtualToReal(abstractVirtualNodeDest, vi, false);
        Node nj = virtualToReal(abstractVirtualNodeDest, vj, true);
        return RealScalar.of(actualDistance(dijkstra, ni, nj)); // dijkstra approach
        
    }

    // TODO make more realistic, step 1: include factor from Euclidean to Network distance, step 2: full shortest path distance (then also add for LP)
    // euclidean approach
    public static double actualDistance(Coord from, Coord to) {
        double dist = CoordUtils.calcEuclideanDistance(from, to);
        return dist;
    }

    // dijkstra approach
    public static double actualDistance(LeastCostPathCalculator dijkstra, Node from, Node to) {
        double dist = 0;
        LeastCostPathCalculator.Path path = EasyDijkstra.executeDijkstra(dijkstra, from, to);
        for (Link link : path.links) {
            dist += link.getLength();
        }
        return dist;
    }

    public static Node virtualToReal(AbstractVirtualNodeDest abstractVirtualNodeDest, VirtualNode vNode, boolean endNode) {
        // TODO: maybe better randomized choice over all links in vi/vj instead of just center
        if (endNode) return abstractVirtualNodeDest.selectLinkSet(vNode, 1).get(0).getToNode();
        return abstractVirtualNodeDest.selectLinkSet(vNode, 1).get(0).getFromNode();
    }

}
