/* *********************************************************************** *
 * project: org.matsim.*
 * SignalizedLinkSpeedFilter
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
package playground.dgrether.koehlerstrehlersignal.network;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.filter.NetworkLinkFilter;


/**
 * @author dgrether
 *
 */
public class SignalizedNodesSpeedFilter implements NetworkLinkFilter {

	private Set<Id<Node>> signalizedNodes;
	private Set<Id<Link>> shortestPathLinkIds;
	private double freeSpeedFilter;
	
	public SignalizedNodesSpeedFilter(Set<Id<Node>> signalizedNodes, Set<Id<Link>> shortestPathLinkIds, double freeSpeedFilter) {
		this.signalizedNodes = signalizedNodes;
		this.shortestPathLinkIds = shortestPathLinkIds;
		this.freeSpeedFilter = freeSpeedFilter;
	}

	@Override
	public boolean judgeLink(Link l) {
		if (this.shortestPathLinkIds.contains(l.getId())){
			return true;
		}
		Id<Node> fromNodeId = l.getFromNode().getId();
		Id<Node> toNodeId = l.getToNode().getId();
		if (this.signalizedNodes.contains(fromNodeId) || this.signalizedNodes.contains(toNodeId)) {
			return true;
		}
		if (l.getFreespeed() > this.freeSpeedFilter) {
			return true;
		}
		return false;
	}

}
