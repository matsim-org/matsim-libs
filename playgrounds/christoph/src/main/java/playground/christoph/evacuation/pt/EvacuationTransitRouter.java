/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTransitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.FastTransitMultiNodeDijkstra;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitTravelDisutility;

/**
 * TODO: make this a real TransitRouter. So far, calcRoute(...) is not implemented.
 * 
 * @author cdobler
 */
public class EvacuationTransitRouter implements TransitRouter {
	
	private final TransitRouterConfig routerConfig;
	private final TransitRouterNetwork routerNetwork;
	private final Collection<TransitRouterNetworkNode> exitNodes;
	private final TransitTravelDisutility transitTravelDisutility;
	private final FastTransitMultiNodeDijkstra dijkstra;
	
	/**
	 * From the toNodes the rescue node / rescue facility can be reached
	 * directly.
	 */
	public EvacuationTransitRouter(TransitRouterConfig routerConfig, TransitRouterNetwork routerNetwork,
			TransitTravelDisutility transitTravelDisutility, Collection<TransitRouterNetworkNode> exitNodes,
			FastTransitMultiNodeDijkstra dijkstra) {
		this.routerConfig = routerConfig;
		this.routerNetwork = routerNetwork;
		this.transitTravelDisutility = transitTravelDisutility;
		this.exitNodes = exitNodes;
		this.dijkstra = dijkstra;
	}
	
	public TransitRouterNetwork getTransitRouterNetwork() {
		return this.routerNetwork;
	}

	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord, double departureTime, Person person) {
		throw new RuntimeException("This is not supported so far.");
	}
	
	public Path calcPath(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
		// find possible start stops
		Collection<TransitRouterNetworkNode> fromNodes = this.routerNetwork.getNearestNodes(fromCoord, this.routerConfig.getSearchRadius());
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.routerNetwork.getNearestNodes(fromCoord, distance + this.routerConfig.getExtensionRadius());
		}

		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			
			double initialTime = calcInitialTime(fromCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
//			double initialCost = initialTime;
			double initialCost = calcInitialCost(fromCoord, node.stop.getStopFacility().getCoord(), person);
			
//			wrappedFromNodes.put(node, new InitialNode(node, initialCost, initialTime + departureTime));
			wrappedFromNodes.put(node, new InitialNode(node, initialCost, initialTime));
		}
		Node wrappedFromNode = this.dijkstra.createImaginaryNode(wrappedFromNodes.values());
		
		// find possible end stops
		Collection<TransitRouterNetworkNode> toNodes = this.routerNetwork.getNearestNodes(toCoord, this.routerConfig.getSearchRadius());
		if (toNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(toCoord);
			double distance = CoordUtils.calcDistance(toCoord, nearestNode.stop.getStopFacility().getCoord());
			toNodes = this.routerNetwork.getNearestNodes(toCoord, distance + this.routerConfig.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {

			double initialTime = calcInitialTime(toCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
//			double initialCost = initialTime;
			double initialCost = calcInitialCost(toCoord, node.stop.getStopFacility().getCoord(), person);
			
//			wrappedToNodes.put(node, new InitialNode(node, initialCost, initialTime + departureTime));
			wrappedToNodes.put(node, new InitialNode(node, initialCost, initialTime));
		}
		Node wrappedToNode = this.dijkstra.createImaginaryNode(wrappedToNodes.values());
		
		// find routes between start and end stops
		Path path = this.dijkstra.calcLeastCostPath(wrappedFromNode, wrappedToNode, departureTime, person, null);
		
		/*
		 * Walk trips to first stop and from last stop are NOT included in the path.
		 */
		if (path != null) {
			InitialNode fromNode = wrappedFromNodes.get(path.nodes.get(0));
			InitialNode toNode = wrappedToNodes.get(path.nodes.get(path.nodes.size() - 1));
			double additionalCosts = fromNode.initialCost + toNode.initialCost;
			double additionalTime = additionalCosts;	// travel time is used as travel cost in the evacuation case!
			path = new Path(path.nodes, path.links, path.travelTime + additionalTime, path.travelCost + additionalCosts);			
		}
		
		return path;
	}
	
	public Path calcExitPath(final Coord fromCoord, final double departureTime, final Person person) {
		// find possible start stops
		Collection<TransitRouterNetworkNode> fromNodes = this.routerNetwork.getNearestNodes(fromCoord, this.routerConfig.getSearchRadius());
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.routerNetwork.getNearestNodes(fromCoord, distance + this.routerConfig.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			
			double initialTime = calcInitialTime(fromCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
			double initialCost = initialTime;
			
//			wrappedFromNodes.put(node, new InitialNode(node, initialCost, initialTime + departureTime));
			wrappedFromNodes.put(node, new InitialNode(node, initialCost, initialTime));
		}
		Node wrappedFromNode = this.dijkstra.createImaginaryNode(wrappedFromNodes.values());
		
		// find possible end stops
		Map<Node, InitialNode> wrappedToExitNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : exitNodes) {
			wrappedToExitNodes.put(node, new InitialNode(node, 0.0, departureTime));
		}
		Node wrappedToExitNode = this.dijkstra.createImaginaryNode(wrappedToExitNodes.values());

		// find routes between start and end stops
		Path path = this.dijkstra.calcLeastCostPath(wrappedFromNode, wrappedToExitNode, departureTime, person, null);
		
		/*
		 * Walk trips to first stop and from last stop are NOT included in the path.
		 */
		if (path != null) {
			InitialNode fromNode = wrappedFromNodes.get(path.nodes.get(0));
			InitialNode toNode = wrappedToExitNodes.get(path.nodes.get(path.nodes.size() - 1));
			double additionalCosts = fromNode.initialCost + toNode.initialCost;
			double additionalTime = additionalCosts;	// travel time is used as travel cost in the evacuation case!
			path = new Path(path.nodes, path.links, path.travelTime + additionalTime, path.travelCost + additionalCosts);			
		}
		
		return path;
	}

	private double calcInitialTime(Coord fromCoord, Coord toCoord, Person person) {
		return this.transitTravelDisutility.getTravelTime(person, fromCoord, toCoord);
	}
	
	private double calcInitialCost(Coord fromCoord, Coord toCoord, Person person) {
		return this.transitTravelDisutility.getTravelDisutility(person, fromCoord, toCoord);
	}
}