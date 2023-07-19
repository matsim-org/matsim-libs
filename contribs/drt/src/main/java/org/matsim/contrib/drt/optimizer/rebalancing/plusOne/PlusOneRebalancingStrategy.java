package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	implements RebalancingStrategy, PassengerRequestScheduledEventHandler, DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler,
	MobsimScopeEventHandler {
	private static final Logger log = LogManager.getLogger(PlusOneRebalancingStrategy.class);

	private final String mode;
	private final Network network;
	private final LinkBasedRelocationCalculator linkBasedRelocationCalculator;

	private final List<Id<Link>> targetLinks = new ArrayList<>();
	private final Map<Id<Request>, Id<Link>> potentialTargetLinks = new HashMap<>();

	public PlusOneRebalancingStrategy(String mode, Network network, LinkBasedRelocationCalculator linkBasedRelocationCalculator) {
		this.mode = mode;
		this.network = network;
		this.linkBasedRelocationCalculator = linkBasedRelocationCalculator;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(toList());

		final List<Id<Link>> copiedTargetLinks;
		synchronized (this) {
			//may happen in parallel to handling PassengerRequestScheduledEvent emitted by UnplannedRequestInserter
			copiedTargetLinks = new ArrayList<>(targetLinks);
			targetLinks.clear(); // clear the target map for next rebalancing cycle
		}

		final List<Link> targetLinkList = copiedTargetLinks.stream().map(network.getLinks()::get).collect(toList());

		log.debug("There are in total " + targetLinkList.size() + " rebalance targets at this time period");
		log.debug("There are " + rebalancableVehicleList.size() + " vehicles that can be rebalanced");

		// calculate the matching result
		return linkBasedRelocationCalculator.calcRelocations(targetLinkList, rebalancableVehicleList);
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			potentialTargetLinks.put(event.getRequestId(), event.getFromLinkId());
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			Id<Link> linkId = potentialTargetLinks.remove(event.getRequestId());
			synchronized (this) {
				// event was emitted by UnplannedRequestInserter, it may arrive during calcRelocations()
				targetLinks.add(linkId);
			}
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			potentialTargetLinks.remove(event.getRequestId());
		}
	}
}
