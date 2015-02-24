/* *********************************************************************** *
 * project: org.matsim.*
 * TtSignalizedNodeShortestPath
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;

/**
 * 
 * @author tthunig
 *
 */
public class TtSignalizedNodeShortestPath {

	private static final Logger log = Logger.getLogger(TtSignalizedNodeShortestPath.class);
	
	/**
	 * 
	 * @param network
	 * @param signalizedNodes
	 * @param useFreeSpeedTravelTime a flag for dijkstras cost function:
	 * if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function
	 * @return
	 */
	public Set<Id<Link>> calcShortestPathLinkIdsBetweenSignalizedNodes(Network network, Set<Id<Node>> signalizedNodes, boolean useFreeSpeedTravelTime){
		//create commodities between all signal-pairs
		DgCommodities signalCommodities = new DgCommodities();
		for (Id<Node> fromSignalId : signalizedNodes){
			for (Id<Node> toSignalId : signalizedNodes){
				if (!fromSignalId.equals(toSignalId)){
					DgCommodity signalCom = new DgCommodity(Id.create("signalCommodity_" + fromSignalId + "-" + toSignalId, DgCommodity.class));
					signalCom.setSourceNode(Id.create(fromSignalId, DgCrossingNode.class), null, 1.0);
					signalCom.setDrainNode(Id.create(toSignalId, DgCrossingNode.class), null);
					signalCommodities.addCommodity(signalCom);
					/*
					 * remark: a DgCommodity needs node to node representation together with link to link representation 
					 * as converter between matsim and ks-model format. 
					 * but in this special case of determine the links of the shortest paths between signalized nodes 
					 * a link to link representation would be misleading and moreover is not used in the router 
					 * TtDgKoehlerStrehler2010Router which calculates the shortest paths in this case.
					 * so we leave the link to link representation empty.
					 */
				}
			}
		}

		//calculate shortest distance paths between all signal-pairs with dijkstra
		TtDgKoehlerStrehler2010Router ttDgKoehlerStrehler2010Router = new TtDgKoehlerStrehler2010Router(useFreeSpeedTravelTime);
		List<Id> invalidSignalCommodities = ttDgKoehlerStrehler2010Router.routeCommodities(network, signalCommodities);
		List<Path> shortestPaths = ttDgKoehlerStrehler2010Router.getShortestPaths();
		if (invalidSignalCommodities.size() != 0) 
			log.warn("There is no valid path between some signals.");
		
		Set<Id<Link>> linkIds = new HashSet<>();
		for (Path path : shortestPaths){
			if (path != null)
				for (Link link : path.links)
					linkIds.add(link.getId());
		}
		return linkIds;
	}
}
