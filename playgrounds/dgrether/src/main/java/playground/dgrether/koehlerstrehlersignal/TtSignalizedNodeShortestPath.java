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
package playground.dgrether.koehlerstrehlersignal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;


public class TtSignalizedNodeShortestPath {

	private static final Logger log = Logger.getLogger(TtSignalizedNodeShortestPath.class);
	
	public Set<Id> calcShortestPathLinkIdsBetweenSignalizedNodes(Network network, Set<Id> signalizedNodes){
		//create commodities between all signal-pairs
		DgCommodities signalCommodities = new DgCommodities();
		for (Id fromSignalId : signalizedNodes){
			for (Id toSignalId : signalizedNodes){
				if (!fromSignalId.equals(toSignalId)){
					DgCommodity signalCom = new DgCommodity(new IdImpl("signalCommodity_" + fromSignalId + "-" + toSignalId));
					signalCom.setSourceNode(fromSignalId, 1.0);
					signalCom.setDrainNode(toSignalId);
					signalCommodities.addCommodity(signalCom);	
				}
			}
		}

		//calculate shortest distance paths between all signal-pairs with dijkstra
		TtDgKoehlerStrehler2010Router ttDgKoehlerStrehler2010Router = new TtDgKoehlerStrehler2010Router();
		List<Id> invalidSignalCommodities = ttDgKoehlerStrehler2010Router.routeCommodities(network, signalCommodities);
		List<Path> shortestPaths = ttDgKoehlerStrehler2010Router.getShortestPaths();
		if (invalidSignalCommodities.size() != 0) 
			log.warn("There is no valid path between some signals.");
		
		Set<Id> linkIds = new HashSet<Id>();
		for (Path path : shortestPaths){
			for (Link link : path.links)
				linkIds.add(link.getId());
		}
		return linkIds;
	}
}
