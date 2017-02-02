/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package contrib.baseline.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;

import java.util.*;

/**
 * Compares expected and observed counts and changes the network outgoing of the count station link.
 * The changes are returned as network change events with a net change factor for each link (if the
 * net change factor is larger than the minimalChangeThreshold).
 *
 * Example:
 *  - Network: link_1 -> link_with_count_station -> link_2
 *  - Expected count: 10, observed count: 8, correctionFactor: 10/8 = 1.25
 *  - changeFactorPerLevel = 0.9, minimalChangeThreshold = 0.1
 *  => flow-capacity of link_with_count_station is multiplied with 1.25
 *  => because the change difference (1.25 - 1 = 0.25) is > threshold (0.1) the next network level is change too
 *  => flow-capacity of link_1 and link_2 are multiplied with (1 + (0.9 * 0.25) =) 1.225
 *  => because the change difference (1.225 - 1 = 0.225) is still > threshold (0.1),
 *     the next level would be changed too, and so on...
 *
 * @author boescpa
 */
public class NetworkAdapter {

	private final Network network;
	private final double changeFactorPerLevel;
	private final double minimalChangeThreshold;

	public NetworkAdapter(Network network) {
		this(network, 0.9, 0.01);

	}

	public NetworkAdapter(Network network, double changeFactorPerLevel, double minimalChangeThreshold) {
		this.network = network;
		this.changeFactorPerLevel = changeFactorPerLevel;
		this.minimalChangeThreshold = minimalChangeThreshold;
	}

	public List<NetworkChangeEvent> identifyNetworkChanges (
			Map<String, Integer> expectedCounts, Map<String, Integer> observedCounts) {

		List<NetworkChangeEvent>  networkChanges = new LinkedList<>();

		for (String countLinkId : expectedCounts.keySet()) {
			Link countLink = network.getLinks().get(Id.create(countLinkId, Link.class));
			double expectedCount = expectedCounts.get(countLinkId);
			double observedCount = observedCounts.get(countLinkId);
			double correctionFactor = expectedCount / observedCount;
			double absCorrection = 1 - correctionFactor;

			Set<Link> currentFront = new HashSet<>();
			// the origin link:
			currentFront.add(countLink);
			networkChanges.add(getNetworkChangeEvents(currentFront, correctionFactor));
			// the remaining links:
			while (Math.abs(absCorrection) > minimalChangeThreshold) {
				Set<Link> newFront = new HashSet<>();
				for (Link currentFrontLink : currentFront) {
					newFront.addAll(currentFrontLink.getFromNode().getInLinks().values());
					newFront.addAll(currentFrontLink.getToNode().getOutLinks().values());
				}
				currentFront = newFront;
				absCorrection *= changeFactorPerLevel;
				correctionFactor = 1 - absCorrection;
				if (!currentFront.isEmpty()) {
					networkChanges.add(getNetworkChangeEvents(currentFront, correctionFactor));
				}
			}
		}
		return simplifyEvents(networkChanges);
	}

	private List<NetworkChangeEvent> simplifyEvents(List<NetworkChangeEvent> networkChanges) {

		Map<Link, Double> netEffects = new HashMap<>();

		for (NetworkChangeEvent event : networkChanges) {
			for (Link link : event.getLinks()) {
				double netEffect = netEffects.containsKey(link)? netEffects.get(link) : 0;
				netEffect += event.getFlowCapacityChange().getValue() - 1;
				netEffects.put(link, netEffect);
			}
		}

		List<NetworkChangeEvent> simplifiedEvents = new ArrayList<>(netEffects.size());
		for (Link link : netEffects.keySet()) {
			if (netEffects.get(link) > minimalChangeThreshold) {
				Set<Link> linkSet = new HashSet<>();
				linkSet.add(link);
				simplifiedEvents.add(getNetworkChangeEvents(linkSet, 1 + netEffects.get(link)));
			}
		}

		return simplifiedEvents;
	}

	private NetworkChangeEvent getNetworkChangeEvents(Set<Link> countLinks, double correctionFactor) {
		NetworkChangeEvent event = new NetworkChangeEvent(0);
		event.addLinks(countLinks);
		event.setFlowCapacityChange(
				new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, correctionFactor));
		return event;
	}

}
