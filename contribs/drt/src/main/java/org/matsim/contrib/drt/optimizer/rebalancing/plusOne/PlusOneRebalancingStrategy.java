package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.EventsManager;

//TODO Using Passenger Request Scheduled event, instead of Person Departure event, 
//will make this implementation more equivalent to the original algorithm in AMoDeus. 
//However, the passenger request scheduled event does not have departure link at this moment. 

/**
 * 
 * @author Chengqi Lu This strategy is based on the Plus One Rebalancing
 *         Algorithm in AMoDeus. At each rebalancing period, the algorithm will
 *         send idling vehicles to the departure places of the request departed
 *         during the time period
 */
public class PlusOneRebalancingStrategy implements RebalancingStrategy, PersonDepartureEventHandler {
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final PlusOneRebalancingParams params;
	private final Network network;

	private final Map<Id<Link>, Integer> targetMap = new HashMap<>();

	public PlusOneRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, Network network,
			PlusOneRebalancingParams params, EventsManager events) {
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.params = params;
		this.network = network;
		events.addHandler(this);
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		System.out.println("Rebalance fleet now: Plus One Rebalancing Algorithm is used"); // TODO delete after testing
		System.out.println("There are " + Integer.toString(targetMap.keySet().size()) + " entries in target map");
		// Initialization
		VehicleInfoCollector vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(Collectors.toList());

		// Get idling vehicles in each zone
		Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
				.groupRebalancableVehicles(rebalancableVehicleList.stream(), time, params.getMinServiceTime());
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return Collections.emptyList();
		}

		// calculate the matching result
		List<Relocation> relocations = matching(rebalancableVehicleList, targetMap, network);
		// clear the target map for next rebalancing cycle
		targetMap.clear();
		return relocations;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("drt")) { // TODO get the string from drt config file
			Id<Link> departureLink = event.getLinkId();
			if (targetMap.containsKey(departureLink)) {
				int newValue = targetMap.get(departureLink) + 1;
				targetMap.put(departureLink, newValue);
			} else {
				targetMap.put(departureLink, 1);
			}

		}
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

}
