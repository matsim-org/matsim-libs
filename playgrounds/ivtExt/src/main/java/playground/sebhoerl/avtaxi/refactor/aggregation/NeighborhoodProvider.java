package playground.sebhoerl.avtaxi.refactor.aggregation;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.*;

public class NeighborhoodProvider {
    final Network network;
    final double radius;

    Map<Link, Set<Link>> neighborhoods = new HashMap<>();

    public NeighborhoodProvider(Network network, double radius) {
        this.network = network;
        this.radius = radius;

        for (Link link : network.getLinks().values()) {
            neighborhoods.put(link, findNeighborhood(link));
        }
    }

    private Set<Link> findNeighborhood(Link link) {
        Queue<Link> unvisited = new LinkedList<>();
        Queue<Double> distances = new LinkedList<>();

        unvisited.add(link);
        distances.add(0.0);

        Set<Link> neighborhood = new HashSet<>();
        Set<Link> outside = new HashSet<>();

        while (unvisited.size() > 0) {
            Link current = unvisited.poll();
            double distance = distances.poll();

            if (distance <= radius) {
                neighborhood.add(current);

                for (Link next : current.getToNode().getOutLinks().values()) {
                    if (!(neighborhood.contains(next) || outside.contains(next))) {
                        unvisited.add(next);
                        distances.add(next.getLength() + distance);
                    }
                }
            } else {
                outside.add(link);
            }
        }

        return neighborhood;
    }
}
