package playground.joel.helpers;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import ch.ethz.idsc.owly.data.GlobalAssert;

/**
 * Created by Joel on 06.07.2017.
 */
public abstract class EasyDijkstra {

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
            @Override
            public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength()/link.getFreespeed();
            }
        };

        return (new FastDijkstraFactory(preProcessData)).createPathCalculator(network, travelMinCost, travelTime);
    }

    public static LeastCostPathCalculator.Path executeDijkstra(LeastCostPathCalculator dijkstra, Node from, Node to) {
        // depending on implementation of traveldisutility and traveltime, starttime, person and vehicle are needed
        GlobalAssert.that(dijkstra!=null);
        GlobalAssert.that(from!=null);
        GlobalAssert.that(to!=null);
        return dijkstra.calcLeastCostPath(from, to, 0.0, null, null);
    }

}
