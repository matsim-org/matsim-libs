package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

import com.google.common.base.Preconditions;

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

	private record Target(Id<Link> link, double scheduledTime) {
	}

	private final Queue<Target> targets = new ConcurrentLinkedQueue<>();
	private final Map<Id<Request>, Id<Link>> potentialTargetLinks = new HashMap<>();

	public PlusOneRebalancingStrategy(String mode, Network network, LinkBasedRelocationCalculator linkBasedRelocationCalculator) {
		this.mode = mode;
		this.network = network;
		this.linkBasedRelocationCalculator = linkBasedRelocationCalculator;
	}

	private double lastCalculationTime = Double.NEGATIVE_INFINITY;

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		var targetLinks = new ArrayList<Link>();
		double prevTime = lastCalculationTime;

		// Because events can be received at the same time we calculate relocations, we want to only consider target links from events that are older
		// than the current time
		while (!targets.isEmpty() && targets.peek().scheduledTime < time) {
			var targetLink = targets.poll();

			// These state checks are meant to ensure we correctly reason about concurrency:
			// 1. ensure all old target links (from previous rebalancing calculations are processed)
			Preconditions.checkState(targetLink.scheduledTime >= lastCalculationTime);
			// 2. ensure target links are sorted by scheduled time
			Preconditions.checkState(targetLink.scheduledTime >= prevTime);

			targetLinks.add(network.getLinks().get(targetLink.link));
			prevTime = targetLink.scheduledTime;
		}

		lastCalculationTime = time;

		log.debug("There are in total " + targetLinks.size() + " rebalance targets at this time period");
		var rebalancableVehicleList = rebalancableVehicles.collect(toList());
		log.debug("There are " + rebalancableVehicleList.size() + " vehicles that can be rebalanced");

		// calculate the matching result
		return linkBasedRelocationCalculator.calcRelocations(targetLinks, rebalancableVehicleList);
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
			if(linkId != null) {
				targets.add(new Target(linkId, event.getTime()));
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
