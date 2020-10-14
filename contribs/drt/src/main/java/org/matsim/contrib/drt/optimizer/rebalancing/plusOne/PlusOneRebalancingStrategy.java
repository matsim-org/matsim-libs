package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

/**
 * This strategy is based on the Plus One Rebalancing Algorithm in AMoDeus.
 * At each rebalancing period, the algorithm will send idling vehicles to the departure places of the request departed
 * during the time period
 *
 * @author Chengqi Lu
 */
public class PlusOneRebalancingStrategy
		implements RebalancingStrategy, PassengerRequestScheduledEventHandler, DrtRequestSubmittedEventHandler,
		PassengerRequestRejectedEventHandler, MobsimScopeEventHandler {
	private static final Logger log = Logger.getLogger(PlusOneRebalancingStrategy.class);

	private final Network network;

	private final LinkBasedRelocationCalculator linkBasedRelocationCalculator;

	private final List<Id<Link>> targetLinkIdList = new ArrayList<>();
	private final Map<Id<Request>, Id<Link>> potentialDrtTripMap = new HashMap<>();

	public PlusOneRebalancingStrategy(Network network, LinkBasedRelocationCalculator linkBasedRelocationCalculator) {
		this.network = network;
		this.linkBasedRelocationCalculator = linkBasedRelocationCalculator;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(toList());

		List<Link> targetLinkList = targetLinkIdList.stream().map(network.getLinks()::get).collect(toList());
		targetLinkIdList.clear(); // clear the target map for next rebalancing cycle

		log.debug("There are in total " + targetLinkList.size() + " rebalance targets at this time period");
		log.debug("There are " + rebalancableVehicleList.size() + " vehicles that can be rebalanced");

		// calculate the matching result
		return linkBasedRelocationCalculator.calcRelocations(targetLinkList, rebalancableVehicleList);
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		potentialDrtTripMap.put(event.getRequestId(), event.getFromLinkId());
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		targetLinkIdList.add(potentialDrtTripMap.remove(event.getRequestId()));
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		potentialDrtTripMap.remove(event.getRequestId());
	}
}
