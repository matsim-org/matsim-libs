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
 * Compares expected and observed counts and changes the network outgoing with the flow spider of the count station link.
 * The changes are returned as network change events with a net change factor for each link (if the
 * net change factor is larger than the minimalChangeThreshold).
 *
 * @author boescpa
 */
public class NetworkAdapter {

	private final Network network;
	private final double minimalChangeThreshold;

	public NetworkAdapter(Network network) {
		this(network, 0.005);
	}

	public NetworkAdapter(Network network, double minimalChangeThreshold) {
		this.network = network;
		this.minimalChangeThreshold = minimalChangeThreshold;
	}

	public List<NetworkChangeEvent> identifyNetworkChanges (
			Map<String, Integer> expectedCounts,
			Map<String, Integer> observedCounts,
			Map<String, Map<String, Double>> changeSpiders) {

		// calculate net effect of count corrections for each affected link
		int maxExpectedCount = Collections.max(expectedCounts.values()); // to scale for importance of count station
		Map<String, Double> netEffects = new HashMap<>();
		for (String countLinkId : expectedCounts.keySet()) {
			double expectedCount = expectedCounts.get(countLinkId);
			double observedCount = observedCounts.get(countLinkId);
			double countCorrection = (expectedCount / observedCount) - 1;
			double stationImportanceCorrection = expectedCount / maxExpectedCount;
			Map<String, Double> countLinkSpider = changeSpiders.get(countLinkId);
			for (String linkId : countLinkSpider.keySet()) {
				double linkCorrection = countLinkSpider.get(linkId) * countCorrection * stationImportanceCorrection;
				double netEffect = netEffects.containsKey(linkId)? netEffects.get(linkId) : 0;
				netEffect += linkCorrection;
				netEffects.put(linkId, netEffect);
			}
		}

		// if net effect is larger than minimalChangeThreshold, create a network change event
		List<NetworkChangeEvent>  networkChanges = new ArrayList<>(netEffects.size());
		for (String linkId : netEffects.keySet()) {
			double netEffect = netEffects.get(linkId);
			if (Math.abs(netEffect) > minimalChangeThreshold) {
				networkChanges.add(getNetworkChangeEvent(linkId, 1 + netEffect));
			}
		}

		return networkChanges;
	}

	private NetworkChangeEvent getNetworkChangeEvent(String linkId, double correctionFactor) {
		NetworkChangeEvent event = new NetworkChangeEvent(0);
		event.addLink(network.getLinks().get(Id.createLinkId(linkId)));
		event.setFlowCapacityChange(
				new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, correctionFactor));
		return event;
	}

}
