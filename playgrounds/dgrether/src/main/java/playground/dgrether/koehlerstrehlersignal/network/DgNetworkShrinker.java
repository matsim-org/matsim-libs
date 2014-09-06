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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.filter.NetworkFilterManager;

import playground.dgrether.EnvelopeLinkStartEndFilter;

import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class DgNetworkShrinker {


	private Set<Id<Node>> signalizedNodes;

	/**
	 * reduce the network size: delete all edges outside the envelope
	 * 
	 * @param net the original network
	 * @param envelope all edges outside this envelope will be removed
	 * @return the filtered network
	 */
	public Network filterLinksOutsideEnvelope(Network net, Envelope envelope) {
		
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		
		//bounding box filter - deletes all edges outside the envelope
		filterManager.addLinkFilter(new EnvelopeLinkStartEndFilter(envelope));
		
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;		
	}
	
	/**
	 * reduce the network size: delete all edges with freespeed <= given value in m/s (freeSpeedFilter), 
	 * that are not on a shortest path (according to travel time or distance respectively) between signalized nodes.
	 * 
	 * @param net the original network
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * @param useFreeSpeedTravelTime a flag for dijkstras cost function:
	 * if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function 
	 * @return the filtered network
	 */
	public Network filterInteriorLinks(Network net, double freeSpeedFilter, boolean useFreeSpeedTravelTime){
		
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		
		//interior link filter - deletes all edges that are not on a shortest path (according to travel time) between signalized nodes
		Set<Id<Link>> shortestPathLinkIds = new TtSignalizedNodeShortestPath().calcShortestPathLinkIdsBetweenSignalizedNodes(net, signalizedNodes, useFreeSpeedTravelTime);
		filterManager.addLinkFilter(new SignalizedNodesSpeedFilter(this.signalizedNodes, shortestPathLinkIds, freeSpeedFilter));
				
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;
	}

	public void setSignalizedNodes(Set<Id<Node>> signalizedNodes) {
		this.signalizedNodes = signalizedNodes;
	}
}

