package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * 
 * @author Chengqi Lu This strategy is based on the Plus One Rebalancing
 *         Algorithm in AMoDeus. At each rebalancing period, the algorithm will
 *         send idling vehicles to the departure places of the request departed
 *         during the time period
 */
public class PlusOneRebalancingStrategy implements RebalancingStrategy, PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {
	private final Network network;

	private final Map<Id<Link>, Integer> targetMap = new HashMap<>();
	private final Map<Id<Person>, Id<Link>> potentialDRTTripsMap = new HashMap<>();

	public PlusOneRebalancingStrategy(Network network,
			PlusOneRebalancingParams params, EventsManager events) {
		this.network = network;
		events.addHandler(this);
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// Get Rebalancable Vehicles
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(Collectors.toList());

		// calculate the matching result
		List<Relocation> relocations = matching(rebalancableVehicleList, targetMap, network);
		
		// clear the target map for next rebalancing cycle
		targetMap.clear();
		
		// Return relocations
		return relocations;
	}

	private List<Relocation> matching(List<? extends DvrpVehicle> rebalancableVehicles,
			Map<Id<Link>, Integer> targetMap, Network network) {
		List<Relocation> relocationList = new ArrayList<>();
		Map<Id<Link>, ? extends Link> linkMap = network.getLinks();
		for (Id<Link> destinationLinkId : targetMap.keySet()) {
			Link destinationLink = linkMap.get(destinationLinkId);
			DvrpVehicle nearestVehicle = findNearestVehicle(destinationLink, rebalancableVehicles);
			if (nearestVehicle == null) {
				break;
			} else {
				relocationList.add(new Relocation(nearestVehicle, destinationLink));
				rebalancableVehicles.remove(nearestVehicle);
			}
		}
		return relocationList;
	}

	private DvrpVehicle findNearestVehicle(Link destinationLink, List<? extends DvrpVehicle> rebalancableVehicles) {
		// First implementation: find the closest vehicles for each link.
		// TODO upgrade the matching with more advanced matching algorithm (e.g.
		// bipartite matching)
		if (rebalancableVehicles.isEmpty()) {
			return null;
		}
		Coord toCoord = destinationLink.getCoord();
		return rebalancableVehicles.stream().min(Comparator.comparing(
				v -> DistanceUtils.calculateSquaredDistance(Schedules.getLastLinkInSchedule(v).getCoord(), toCoord)))
				.get();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		potentialDRTTripsMap.put(event.getPersonId(), event.getFromLinkId());
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<Link> departureLink = potentialDRTTripsMap.get(personId);
		if (targetMap.containsKey(departureLink)) {
			int newValue = targetMap.get(departureLink) + 1;
			targetMap.put(departureLink, newValue);
		} else {
			targetMap.put(departureLink, 1);
		}
	}
	
	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		potentialDRTTripsMap.remove(event.getPersonId());
	}

}
