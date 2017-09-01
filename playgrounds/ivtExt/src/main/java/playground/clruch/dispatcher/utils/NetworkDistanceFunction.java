/**
 * @author Claudio Ruch
 */
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;

import ch.ethz.idsc.owly.data.GlobalAssert;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.joel.helpers.EasyDijkstra;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
public class NetworkDistanceFunction implements DistanceFunction {
    private final LeastCostPathCalculator dijkstra;
    
    public NetworkDistanceFunction(Network network){
        dijkstra =EasyDijkstra.prepDijkstra(network);        
    }


    @Override
    public double getDistance(RoboTaxi robotaxi, AVRequest avrequest) {

        Node from = robotaxi.getDivertableLocation().getFromNode();
        Node to = avrequest.getFromLink().getFromNode();

        return distNetwork(from, to);

    }

    @Override
    public double getDistance(RoboTaxi robotaxi, Link link) {

        Node from = robotaxi.getDivertableLocation().getFromNode();
        Node to = link.getFromNode();

        return distNetwork(from, to);

    }

    private double distNetwork(Node from, Node to) {
        GlobalAssert.that(from!=null);
        GlobalAssert.that(to!=null);
        double dist = 0.0;
        LeastCostPathCalculator.Path path = EasyDijkstra.executeDijkstra(dijkstra, from, to);
        for (Link link : path.links) {
            dist += link.getLength();
        }
        return dist;
    }

}
