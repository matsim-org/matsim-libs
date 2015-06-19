/* *********************************************************************** *
 * project: org.matsim.*
 * FastTransitRouterImpl.java
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

package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.util.FastTransitDijkstraFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * Not thread-safe because MultiNodeDijkstra is not. Does not expect the TransitSchedule to change once constructed! michaz '13
 * 
 * @author cdobler
 */
public class FastTransitRouterImpl implements TransitRouter {

	private final TransitRouterNetwork transitNetwork;

	private final FastTransitMultiNodeDijkstra dijkstra;
	private final TransitRouterConfig config;
	private final TransitTravelDisutility transitTravelDisutility;
	private final TravelTime travelTime;

	private final PreparedTransitSchedule preparedTransitSchedule; 

	public FastTransitRouterImpl(
			final TransitRouterConfig config, 
			final PreparedTransitSchedule preparedTransitSchedule, 
			final TransitRouterNetwork routerNetwork, 
			final TravelTime travelTime, 
			final TransitTravelDisutility travelDisutility,
			final FastTransitDijkstraFactory dijkstraFactory) {
		this.config = config;
		this.transitNetwork = routerNetwork;
		this.travelTime = travelTime;
		this.transitTravelDisutility = travelDisutility;
		TransitTravelDisutilityWrapper wrapper = new TransitTravelDisutilityWrapper(transitTravelDisutility);
		this.dijkstra = (FastTransitMultiNodeDijkstra) dijkstraFactory.createPathCalculator(this.transitNetwork, 
				wrapper, this.travelTime);
		wrapper.setCustomDataManager(dijkstra.getCustomDataManager());
		this.preparedTransitSchedule = preparedTransitSchedule;
		//		this.dijkstra = null; // enable to save memory if no routing should be done
	}

	private Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime){
		Collection<TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, this.config.getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcDistance(coord, nearestNode.stop.getStopFacility().getCoord());
			nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.config.getExtensionRadius());
		}
		
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getStopFacility().getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
//			wrappedNearestNodes.put(node, new InitialNode(node, initialCost, initialTime + departureTime));
			wrappedNearestNodes.put(node, new InitialNode(node, initialCost, initialTime));	// only use travel time!
		}
		return wrappedNearestNodes;
	}
	
	private double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return this.transitTravelDisutility.getTravelTime(person, coord, toCoord);
	}
	
	private double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return this.transitTravelDisutility.getTravelDisutility(person, coord, toCoord);
	}

	@Override
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromCoord, departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNodes(person, toCoord, departureTime);

		// create imaginary nodes
		Node imaginaryFromNode = this.dijkstra.createImaginaryNode(wrappedFromNodes.values(), fromCoord);
		Node imaginarytoNode = this.dijkstra.createImaginaryNode(wrappedToNodes.values(), toCoord);
		
		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(imaginaryFromNode, imaginarytoNode, departureTime, person, null);

		if (p == null) {
			return null;
		}

		double directWalkCost = getWalkDisutility(person, fromCoord, toCoord);
		double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;

		if (directWalkCost < pathCost) {
			return this.createDirectWalkLegList(null, fromCoord, toCoord);
		}
		return convertPathToLegList( departureTime, p, fromCoord, toCoord, person ) ;
	}

	private List<Leg> createDirectWalkLegList(Person person, Coord fromCoord, Coord toCoord) {
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = new LegImpl(TransportMode.transit_walk);
		double walkTime = getWalkTime(person, fromCoord, toCoord);
		Route walkRoute = new GenericRouteImpl(null, null);
		leg.setRoute(walkRoute);
		leg.setTravelTime(walkTime);
		legs.add(leg);
		return legs;
	}

	protected List<Leg> convertPathToLegList( double departureTime, Path p, Coord fromCoord, Coord toCoord, Person person ) {
		// yy there could be a better name for this method.  kai, apr'10

		// now convert the path back into a series of legs with correct routes
		double time = departureTime;
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = null;

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		TransitRouterNetworkLink prevLink = null;
		int transitLegCnt = 0;
		for (Link link : p.links) {
			TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
			if (l.line == null) {
				TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset = (((TransitRouterNetworkLink) link).getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? ((TransitRouterNetworkLink) link).fromNode.stop.getArrivalOffset() : ((TransitRouterNetworkLink) link).fromNode.stop.getDepartureOffset();
					double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					transitLegCnt++;
					accessStop = egressStop;
				}
				line = null;
				route = null;
				transitRouteStart = null;
			} else {
				if (l.route != route) {
					// the line changed
					TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkLink) link).getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = getWalkTime(person, accessStop.getCoord(), egressStop.getCoord()); // CoordUtils.calcDistance(accessStop.getCoord(), egressStop.getCoord()) / this.config.getBeelineWalkSpeed();
								Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
								leg.setRoute(walkRoute);
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							} else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = getWalkTime(person, fromCoord, egressStop.getCoord());
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							}
						}
					}
					line = l.line;
					route = l.route;
					accessStop = egressStop;
				}
			}
			prevLink = l;
		}
		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
					(prevLink).toNode.stop.getArrivalOffset()
					: (prevLink).toNode.stop.getDepartureOffset();
					double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);

					legs.add(leg);
					transitLegCnt++;
					accessStop = egressStop;
		}
		if (prevLink != null) {
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime;
			if (accessStop == null) {
				walkTime = getWalkTime(person, fromCoord, toCoord);
			} else {
				walkTime = getWalkTime(person, accessStop.getCoord(), toCoord);
			}
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime = getWalkTime(person, fromCoord, toCoord);
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		return legs;
	}

	public TransitRouterNetwork getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetwork getTransitNetwork() {
		return transitNetwork;
	}

	protected FastTransitMultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
	}

}
