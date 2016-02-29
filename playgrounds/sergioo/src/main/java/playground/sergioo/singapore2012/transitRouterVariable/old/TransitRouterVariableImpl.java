/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouterVariableImpl.java
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

package playground.sergioo.singapore2012.transitRouterVariable.old;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.sergioo.singapore2012.transitRouterVariable.old.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.old.TransitRouterNetworkWW.TransitRouterNetworkNode;

public class TransitRouterVariableImpl implements TransitRouter {

	private final TransitRouterNetworkWW transitNetwork;

	private final MultiNodeDijkstra dijkstra;
	private final TransitRouterConfig config;
	private final TransitRouterNetworkTravelTimeAndDisutility ttCalculator;

	public TransitRouterVariableImpl(final TransitRouterConfig config,
			final TransitRouterNetworkTravelTimeAndDisutility ttCalculator, final TransitRouterNetworkWW routerNetwork, final Network network) {
		this.config = config;
		this.transitNetwork = routerNetwork;
		this.ttCalculator = ttCalculator;
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.ttCalculator, this.ttCalculator);
	}
	
	private Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime){
		Collection<TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, this.config.getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getStopFacility().getCoord());
			nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.config.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getStopFacility().getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestNodes;
	}
	
	private double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getTravelTime(person, coord, toCoord);
	}
	
	private double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getTravelDisutility(person, coord, toCoord);
	}
	
	@Override
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromCoord, departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes  = this.locateWrappedNearestTransitNodes(person, toCoord, departureTime);

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);
		if (p == null) {
			return null;
		}

		double directWalkCost = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / this.config.getBeelineWalkSpeed() * ( 0 - this.config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
		if (directWalkCost < pathCost) {
			List<Leg> legs = new ArrayList<Leg>();
			Leg leg = new LegImpl(TransportMode.transit_walk);
			double walkDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
			Route walkRoute = new GenericRouteImpl(null, null);
			walkRoute.setDistance(walkDistance);
			leg.setRoute(walkRoute);
			leg.setTravelTime(walkDistance/this.config.getBeelineWalkSpeed());
			legs.add(leg);
			return legs;
		}

		return convertPathToLegList( departureTime, p, fromCoord, toCoord, person ) ;
	}
	
	public Path calcPathRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
		// find possible start stops
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromCoord, departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes  = this.locateWrappedNearestTransitNodes(person, toCoord, departureTime);
		// find routes between start and end stops
		return this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);
	}
	
	protected List<Leg> convertPathToLegList( double departureTime, Path p, Coord fromCoord, Coord toCoord, Person person) {
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg;
		double walkDistance, walkWaitTime, travelTime = 0;
		Route walkRoute;
		Coord coord = fromCoord;
		TransitRouteStop stop = null;
		double time = departureTime;
		for (Link link : p.links) {
			TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
			if(l.route!=null) {
				//in line link
				double ttime = ttCalculator.getLinkTravelTime(l, time, person, null);
				travelTime += ttime;
				time += ttime;
			}
			else if(l.fromNode.route!=null) {
				//inside link
				leg = new LegImpl(TransportMode.pt);
				ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(stop.getStopFacility(), l.fromNode.line, l.fromNode.route, l.fromNode.stop.getStopFacility());
				leg.setRoute(ptRoute);
				leg.setTravelTime(travelTime);
				legs.add(leg);
				travelTime = 0;
				stop = l.fromNode.stop;
				coord = l.fromNode.stop.getStopFacility().getCoord();
			}
			else if(l.toNode.route!=null) {
				//wait link
				leg = new LegImpl(TransportMode.transit_walk);
				walkDistance = CoordUtils.calcEuclideanDistance(coord, l.toNode.stop.getStopFacility().getCoord()); 
				walkWaitTime = walkDistance/this.config.getBeelineWalkSpeed()+ttCalculator.getLinkTravelTime(l, time+walkDistance/this.config.getBeelineWalkSpeed(), person, null);
				walkRoute = new GenericRouteImpl(stop==null?null:stop.getStopFacility().getLinkId(), l.toNode.stop.getStopFacility().getLinkId());
				walkRoute.setDistance(walkDistance);
				leg.setRoute(walkRoute);
				leg.setTravelTime(walkWaitTime);
				legs.add(leg);
				stop = l.toNode.stop;
				time += walkWaitTime;
			}
			
		}
		leg = new LegImpl(TransportMode.transit_walk);
		walkDistance = CoordUtils.calcEuclideanDistance(coord, toCoord); 
		walkWaitTime = walkDistance/this.config.getBeelineWalkSpeed();
		walkRoute = new GenericRouteImpl(stop==null?null:stop.getStopFacility().getLinkId(), null);
		walkRoute.setDistance(walkDistance);
		leg.setRoute(walkRoute);
		leg.setTravelTime(walkWaitTime);
		legs.add(leg);
		return legs;
	}

	public TransitRouterNetworkWW getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetworkWW getTransitNetwork() {
		return transitNetwork;
	}

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
	}

}
