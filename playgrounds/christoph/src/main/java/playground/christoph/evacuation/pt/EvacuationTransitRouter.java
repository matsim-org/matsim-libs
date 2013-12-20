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
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
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

	/*
	 * Factor to increase a beeline distance. We assume that
	 * beelineDistance * factor ~ real world distance.
	 */
	private final double beelineDistanceFactor;
		
	private final TransitRouterConfig routerConfig;
	private final TransitRouterNetwork routerNetwork;
	private final Collection<TransitRouterNetworkNode> toNodes;
	private final TravelTime walkTravelTime;
	private final MultiNodeDijkstra dijkstra;
		
	/**
	 * From the toNodes the rescue node / rescue facility can be reached
	 * directly.
	 * 
	 * @param config
	 * @param routerNetwork
	 * @param travelTime
	 * @param travelDisutility
	 * @param toNodes
	 */
	public EvacuationTransitRouter(Config config, TransitRouterConfig routerConfig, TransitRouterNetwork routerNetwork,
			TravelTime transitTravelTime, TransitTravelDisutility travelDisutility, Collection<TransitRouterNetworkNode> toNodes,
			TravelTime walkTravelTime) {
		this.beelineDistanceFactor = config.plansCalcRoute().getBeelineDistanceFactor();
		this.routerConfig = routerConfig;
		this.routerNetwork = routerNetwork;
		this.toNodes = toNodes;
		this.walkTravelTime = walkTravelTime;
		
		this.dijkstra = new MultiNodeDijkstra(routerNetwork, travelDisutility, transitTravelTime);
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
		Collection<TransitRouterNetworkNode> fromNodes = this.routerNetwork.getNearestNodes(fromCoord, this.routerConfig.searchRadius);
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.routerNetwork.getNearestNodes(fromCoord, distance + this.routerConfig.extensionRadius);
		}
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			
			double initialTime = calcInitialTime(fromCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
			double initialCost = initialTime;
			
			wrappedFromNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		
		// find possible end stops
		Collection<TransitRouterNetworkNode> toNodes = this.routerNetwork.getNearestNodes(toCoord, this.routerConfig.searchRadius);
		if (toNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(toCoord);
			double distance = CoordUtils.calcDistance(toCoord, nearestNode.stop.getStopFacility().getCoord());
			toNodes = this.routerNetwork.getNearestNodes(toCoord, distance + this.routerConfig.extensionRadius);
		}
		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {

			double initialTime = calcInitialTime(toCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
			double initialCost = initialTime;
			
			wrappedToNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		
		// find routes between start and end stops
		Path path = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);
		
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
		Collection<TransitRouterNetworkNode> fromNodes = this.routerNetwork.getNearestNodes(fromCoord, this.routerConfig.searchRadius);
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.routerNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.routerNetwork.getNearestNodes(fromCoord, distance + this.routerConfig.extensionRadius);
		}
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			
			double initialTime = calcInitialTime(fromCoord, node.stop.getStopFacility().getCoord(), person);
			
			// in the evacuation case we use travel time as travel costs
			double initialCost = initialTime;
			
			wrappedFromNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find possible end stops
		Map<Node, InitialNode> wrappedToExitNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {
			wrappedToExitNodes.put(node, new InitialNode(0.0, departureTime));
		}

		// find routes between start and end stops
		Path path = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToExitNodes, person);

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

		// Use person speed instead of beeline walk speed
		Link dummyLink = new DummyLink(this.beelineDistanceFactor, fromCoord, toCoord);
		double initialTime = this.walkTravelTime.getLinkTravelTime(dummyLink, 0.0, person, null);
		
		return initialTime;
	}
	
	private static class DummyLink implements Link {

		private final static Id id = new IdImpl("dummyLink");
		private final double length;
		private final Node fromNode;
		private final Node toNode;
		
		public DummyLink(double beelineDistanceFactor, Coord fromCoord, Coord toCoord) {
			this.length = CoordUtils.calcDistance(fromCoord, toCoord) * beelineDistanceFactor;
			this.fromNode = new DummyNode(fromCoord);
			this.toNode = new DummyNode(toCoord);
		}
		
		@Override
		public Coord getCoord() { return null; }

		@Override
		public Id getId() { return id; }

		@Override
		public boolean setFromNode(Node node) { return false; }

		@Override
		public boolean setToNode(Node node) { return false; }

		@Override
		public Node getToNode() {
			return this.toNode;
		}

		@Override
		public Node getFromNode() {
			return this.fromNode;
		}

		@Override
		public double getLength() {
			return this.length;
		}

		@Override
		public double getNumberOfLanes() { return 0; }

		@Override
		public double getNumberOfLanes(double time) { return 0; }

		@Override
		public double getFreespeed() { return 0; }

		@Override
		public double getFreespeed(double time) { return 0; }

		@Override
		public double getCapacity() { return 0; }

		@Override
		public double getCapacity(double time) { return 0; }

		@Override
		public void setFreespeed(double freespeed) { }

		@Override
		public void setLength(double length) { }

		@Override
		public void setNumberOfLanes(double lanes) { }

		@Override
		public void setCapacity(double capacity) { }

		@Override
		public void setAllowedModes(Set<String> modes) { }

		@Override
		public Set<String> getAllowedModes() { return null; }
	}
	
	private static class DummyNode implements Node {

		private final Coord coord;
		
		public DummyNode(Coord coord) {
			this.coord = coord;
		}
		
		@Override
		public Coord getCoord() {
			return coord;
		}

		@Override
		public Id getId() { return null; }

		@Override
		public boolean addInLink(Link link) { return false; }

		@Override
		public boolean addOutLink(Link link) { return false; }

		@Override
		public Map<Id, ? extends Link> getInLinks() { return null; }

		@Override
		public Map<Id, ? extends Link> getOutLinks() { return null; }
	}

}
