package playground.sebhoerl.avtaxi.refactor.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Set;

public class NeighborhoodBasedDijkstra extends Dijkstra {
    private Set<Link> neighborhood;

    public NeighborhoodBasedDijkstra(Network network, TravelDisutility costFunction, TravelTime timeFunction) {
        super(network, costFunction, timeFunction);
    }

    public void setNeighborhood(Set<Link> neighborhood) {
        this.neighborhood = neighborhood;
    }

    protected boolean canPassLink(final Link link) {
        if (neighborhood != null) {
            return neighborhood.contains(link);
        }

        return super.canPassLink(link);
    }
}
