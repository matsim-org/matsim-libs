package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
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
import org.matsim.core.events.MobsimScopeEventHandler;

/**
 *
 * @author Chengqi Lu This strategy is based on the Plus One Rebalancing
 *         Algorithm in AMoDeus. At each rebalancing period, the algorithm will
 *         send idling vehicles to the departure places of the request departed
 *         during the time period
 */
public class PlusOneRebalancingStrategy implements RebalancingStrategy, PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler, MobsimScopeEventHandler {
	private static final Logger log = Logger.getLogger(PlusOneRebalancingStrategy.class);
	private final Network network;

	private final ZoneFreeRelocationCalculator zoneFreeRelocationCalculator;

	private final List<Id<Link>> targetLinkIDList = new ArrayList<>();
	private final Map<Id<Person>, Id<Link>> potentialDRTTripsMap = new HashMap<>();

	public PlusOneRebalancingStrategy(Network network, ZoneFreeRelocationCalculator zoneFreeRelocationCalculator) {
		this.network = network;
		this.zoneFreeRelocationCalculator = zoneFreeRelocationCalculator;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// Get Rebalancable Vehicles
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(Collectors.toList());

		// calculate the matching result
		List<Link> targetLinkList = linkIDsToLinks(targetLinkIDList);
		log.debug("There are in total " + targetLinkList.size() + " rebalance targets at this time period");
		log.debug("There are " + rebalancableVehicleList.size() + " vehicles that can be rebalanced");
		List<Relocation> relocations = zoneFreeRelocationCalculator.calcRelocations(targetLinkList,
				rebalancableVehicleList);

		// clear the target map for next rebalancing cycle
		targetLinkIDList.clear();

		// Return relocations
		return relocations;
	}

	private List<Link> linkIDsToLinks(List<Id<Link>> targetLinkIDList) {
		List<Link> targetLinkList = new ArrayList<>();
		Map<Id<Link>, ? extends Link> linkMap = network.getLinks();
		for (Id<Link> linkId : targetLinkIDList) {
			targetLinkList.add(linkMap.get(linkId));
		}
		return targetLinkList;
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		potentialDRTTripsMap.put(event.getPersonId(), event.getFromLinkId());
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<Link> departureLink = potentialDRTTripsMap.get(personId);
		targetLinkIDList.add(departureLink);
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		potentialDRTTripsMap.remove(event.getPersonId());
	}

}
