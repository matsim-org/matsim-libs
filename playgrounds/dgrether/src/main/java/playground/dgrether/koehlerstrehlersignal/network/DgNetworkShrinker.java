/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusSmallNetworkGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.EnvelopeLinkStartEndFilter;

import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class DgNetworkShrinker {


	private Set<Id> signalizedNodes;

	/**
	 * reduce the network size: delete all edges
	 * 1. outside the envelope
	 * 2. with freespeed <= 10 m/s, that are not on a shortest path (according to travel time) between signalized nodes.
	 * 
	 * @param net the original network
	 * @param envelope
	 * @param networkCrs
	 * @return the small network
	 */
	public Network createSmallNetwork(Network net, Envelope envelope, CoordinateReferenceSystem networkCrs) {
		
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		
		//bounding box filter - deletes all edges outside the envelope
		filterManager.addLinkFilter(new EnvelopeLinkStartEndFilter(envelope));
		
		//interior link filter - deletes all edges that are not on a shortest path (according to travel time) between signalized nodes
		Set<Id> shortestPathLinkIds = new TtSignalizedNodeShortestPath().calcShortestPathLinkIdsBetweenSignalizedNodes(net, signalizedNodes);
		filterManager.addLinkFilter(new SignalizedNodesSpeedFilter(this.signalizedNodes, shortestPathLinkIds));
		
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;		
	}

	public void setSignalizedNodes(Set<Id> signalizedNodes) {
		this.signalizedNodes = signalizedNodes;
	}
}

