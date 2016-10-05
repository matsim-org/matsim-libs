package playground.sebhoerl.avtaxi.refactor.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class NeighborhoodProvider {
    final Network network;
    final TravelDisutility travelDisutility;
    final TravelTime travelTime;

    public NeighborhoodProvider(Network network, TravelDisutility travelDisutility, TravelTime travelTime) {
        this.network = network;
        this.travelDisutility = travelDisutility;
        this.travelTime = travelTime;
    }

    public Neighborhood getNeighborhood(Link seed, double threshold, double time, Person person, Vehicle vehicle) {
        Set<Link> neighborhood = new HashSet<>();

        Queue<Link> unvisited = new LinkedList<>();
        Queue<Double> costs = new LinkedList<>();
        Queue<Double> times = new LinkedList<>();

        unvisited.add(seed);
        neighborhood.add(seed);

        costs.add(0.0);
        times.add(time);

        while (unvisited.size() > 0) {
            Link currentLink = unvisited.poll();
            double currentTime = times.poll();
            double currentCost = costs.poll();

            currentCost += travelDisutility.getLinkTravelDisutility(currentLink, currentTime, person, vehicle);
            currentTime += travelTime.getLinkTravelTime(currentLink, currentTime, person, vehicle);

            if (currentCost <= threshold) {
                for (Link nextLink : currentLink.getToNode().getOutLinks().values()) {
                    if (!neighborhood.contains(nextLink)) {
                        unvisited.add(nextLink);
                        costs.add(currentCost);
                        times.add(currentTime);
                    }
                }
            }
        }

        return new Neighborhood(neighborhood);
    }
}
