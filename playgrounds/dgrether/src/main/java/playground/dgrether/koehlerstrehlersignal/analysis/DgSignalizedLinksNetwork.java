/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalizedLinksNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * *********************************************************************** */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.SignalSystem;

import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class DgSignalizedLinksNetwork {

	
	public Network createSmallNetwork(Network net, SignalSystemsData data) {
		Map<Id<SignalSystem>, Set<Id<Link>>> signalsToLinks = DgSignalsUtils.calculateSignalizedLinksPerSystem(data);
		final Set<Id<Link>> signalizedLinks = new HashSet<>();
		for (Set<Id<Link>> linkSet : signalsToLinks.values()){
			signalizedLinks.addAll(linkSet);
		}
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (signalizedLinks.contains(l.getId())){
					return true;
				}
				return false;
			}
		});

		Network newNetwork = filterManager.applyFilters();
		if (signalizedLinks.size() != newNetwork.getLinks().size()){
			throw new IllegalStateException("Network should contain all signalized links but not more!");
		}
		return newNetwork;		
	}
}
