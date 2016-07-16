/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouter.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

public class ParkingRouter {

	private static final Logger log = Logger.getLogger(ParkingRouter.class);
	
	private final Network network;
	private final TravelTime travelTime;
	private final TransitTravelDisutility travelDisutility;
	private final int nodesToCheck;
	
	private final MultiNodeDijkstra dijkstra;
	
	public ParkingRouter(Network network, TravelTime travelTime, TransitTravelDisutility travelDisutility, int nodesToCheck) {
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		
		if (nodesToCheck > 0) this.nodesToCheck = nodesToCheck;
		else this.nodesToCheck = 1;
		
		this.dijkstra = new MultiNodeDijkstra(network, travelDisutility, travelTime);
	}
	
	public void adaptRoute(NetworkRoute route, Link startLink, double time, Person person, 
			Vehicle vehicle, CustomDataManager dataManager) {
		
		Node startNode = startLink.getToNode();
		
		List<Id> routeLinkIds = new ArrayList<Id>();
		routeLinkIds.addAll(route.getLinkIds());
		routeLinkIds.add(route.getEndLinkId());
		
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();		
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		
		fromNodes.put(startNode, new InitialNode(0.0, time));
		
		/*
		 * Define how many nodes of the route should be checked. By default,
		 * this is limited by nodesToCheck. For short routes, it is limited
		 * by the route's length.
		 */
		int n = Math.min(nodesToCheck, routeLinkIds.size());
		
		double postCosts = 0.0;
		Map<Node, Integer> nodeIndices = new HashMap<Node, Integer>();
//		Node[] toNodes = new Node[n];
		for (int i = n-1; i >= 0; i--) {
			Id linkId = routeLinkIds.get(i);
			Link link = this.network.getLinks().get(linkId);
			Node fromNode = link.getFromNode();
			toNodes.put(fromNode, new InitialNode(postCosts, time));
			nodeIndices.put(fromNode, i);
			
			// add costs of current link to previous node
			postCosts += travelDisutility.getLinkTravelDisutility(link, time, person, vehicle, dataManager);
		}
		
		Path path = this.dijkstra.calcLeastCostPath(fromNodes, toNodes, person);
		
		for (Link link : path.links) {
			log.info(link.getId());
		}
		
		for (Node node : path.nodes) {
			log.info(node.getId());
		}
		
		/*
		 * Merge old and new route.
		 */
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		
		// new links
		for (Link link : path.links) linkIds.add(link.getId());
		
		// existing links
		Node lastNode = path.nodes.get(path.nodes.size() - 1);
		int mergeIndex = nodeIndices.get(lastNode);
		linkIds.addAll(route.getLinkIds().subList(mergeIndex, route.getLinkIds().size()));
		
		route.setLinkIds(startLink.getId(), linkIds, route.getEndLinkId());
	}
}
