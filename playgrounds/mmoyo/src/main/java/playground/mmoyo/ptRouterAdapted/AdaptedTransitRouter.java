/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * changes made to the original org.matsim.pt.router.TransitRouter: -stop search -transfer links creation -direct walk optional
 * -uses myTransitRouterConfig Manuel apr10
 */
public class AdaptedTransitRouter extends TransitRouter {

	public AdaptedTransitRouter(MyTransitRouterConfig myTRConfig, final TransitSchedule schedule) {   //creado una vez
		super(schedule, myTRConfig, new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig));    
		//attention : the transit network is created first in the upper class   with "this.adaptedTransitNetwork = buildNetwork()";
	}

	@Override
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {

		// progressive stop search*
		Collection<TransitRouterNetworkNode> fromNodes;
		double searchRadius = this.getConfig().searchRadius;
		do {
			fromNodes = this.getTransitRouterNetwork().getNearestNodes(fromCoord, searchRadius); // walkRange
			searchRadius += this.getConfig().extensionRadius;
		} while (fromNodes.size() < ((MyTransitRouterConfig)this.getConfig()).minStationsNum);
		// /////////////////////////////////////////////////

		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.getConfig().beelineWalkSpeed;
			double initialCost = -initialTime * this.getConfig().marginalUtilityOfTravelTimeWalk; // yyyy check sign!!! kai, apr'10
			wrappedFromNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// progressive stop search*
		Collection<TransitRouterNetworkNode> toNodes;
		searchRadius = this.getConfig().searchRadius;
		do {
			toNodes = this.getTransitNetwork().getNearestNodes(toCoord, searchRadius); // walkRange
			searchRadius += this.getConfig().extensionRadius;
		} while (toNodes.size() < ((MyTransitRouterConfig)this.getConfig()).minStationsNum);
		// //////////////////////////////////////////////////

		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {
			double distance = CoordUtils.calcDistance(toCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.getConfig().beelineWalkSpeed;
			double initialCost = -initialTime * this.getConfig().marginalUtilityOfTravelTimeWalk; // yyyy check sign!!! kai, apr'10
			wrappedToNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find routes between start and end stops
		Path p = this.getDijkstra().calcLeastCostPath(wrappedFromNodes, wrappedToNodes);

		if (p == null) {
			return null;
		}

		// optional direct walk *
		if (((MyTransitRouterConfig)this.getConfig()).allowDirectWalks) {
			double directWalkCost = -CoordUtils.calcDistance(fromCoord, toCoord) / this.getConfig().beelineWalkSpeed
					* this.getConfig().marginalUtilityOfTravelTimeWalk;
			double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost
					+ wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
			if (directWalkCost < pathCost) {
				List<Leg> legs = new ArrayList<Leg>();
				Leg leg = new LegImpl(TransportMode.transit_walk);
				double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) / this.getConfig().beelineWalkSpeed;
				Route walkRoute = new GenericRouteImpl(null, null);
				leg.setRoute(walkRoute);
				leg.setTravelTime(walkTime);
				legs.add(leg);
				return legs;
			}
		}
		
		return convert( departureTime, p, fromCoord, toCoord ) ;
	}

	/**necessary to override since it uses a different algo than marcel.  kai, apr'10 
	 * 
	 */
	@Override
	protected TransitRouterNetwork buildNetwork() {
				
		final TransitRouterNetwork network = new TransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : this.getSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					this.getNodeMappings().put(node, stop.getStopFacility());
					if (prevNode != null) {
						TransitRouterNetworkLink link = network.createLink(prevNode, node, route, line);
						this.getLinkMappings().put(link, new Tuple<TransitLine, TransitRoute>(line, route));
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...
		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> toBeAdded = new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();

		int transfers = 0;

		// reduced creation of transferlinks*
		for (TransitRouterNetworkNode centerNode : network.getNodes().values()) {
			for (TransitRouterNetworkNode nearNode : network.getNearestNodes(centerNode.getCoord(),
					this.getConfig().beelineWalkConnectionDistance)) {
				if (centerNode != nearNode && centerNode.line != nearNode.line) { // || centerNode.stop.getStopFacility() !=
					// nearNode.stop.getStopFacility() this
					// condition creates more transfer links
					if (centerNode.route.getStops().get(0) != centerNode.stop
							&& nearNode.route.getStops().get(nearNode.route.getStops().size() - 1) != nearNode.stop) {
						toBeAdded.add(new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(centerNode, nearNode));
						transfers++;
					}
				}
			}
		}
		// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : toBeAdded) {
			network.createLink(tuple.getFirst(), tuple.getSecond(), null, null);
		}

		System.out.println("adapted router network statistics:");
		System.out.println(" # nodes: " + network.getNodes().size());
		System.out.println(" # links: " + network.getLinks().size());
		System.out.println(" # transfer links: " + toBeAdded.size());
		System.out.println(" # transfers:" + transfers);
		return network;
	}

	// the procedures with * were adapted
}
