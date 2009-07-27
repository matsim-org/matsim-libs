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

package playground.marcel.pt.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.router.MultiNodeDijkstra.InitialNode;
import playground.marcel.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import playground.marcel.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class TransitRouter {

	private final TransitSchedule schedule;
	private final TransitRouterNetwork transitNetwork;
	private final TransitRouterNetworkWrapper wrappedNetwork;
	private final Map<TransitRouterNetwork.TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>> linkMappings;
	private final Map<TransitRouterNetwork.TransitRouterNetworkNode, TransitStopFacility> nodeMappings;

	private final MultiNodeDijkstra dijkstra;
	private final TransitRouterConfig config;

	public TransitRouter(final TransitSchedule schedule) {
		this(schedule, new TransitRouterConfig());
	}

	public TransitRouter(final TransitSchedule schedule, final TransitRouterConfig config) {
		this.schedule = schedule;
		this.config = config;
		this.linkMappings = new HashMap<TransitRouterNetwork.TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>>();
		this.nodeMappings = new HashMap<TransitRouterNetwork.TransitRouterNetworkNode, TransitStopFacility>();
		this.transitNetwork = buildNetwork();
		this.wrappedNetwork = new TransitRouterNetworkWrapper(this.transitNetwork);

		TransitRouterNetworkTravelTimeCost c = new TransitRouterNetworkTravelTimeCost(this.config);
		this.dijkstra = new MultiNodeDijkstra(this.wrappedNetwork, c, c);
//		new NetworkWriter(this.wrappedNetwork, "wrappedNetwork.xml").write();
	}

	public List<Id> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		// find possible start stops
		TransitRouterNetwork.TransitRouterNetworkNode fromNode = this.transitNetwork.getNearestNode(fromCoord);
		TransitRouterNetworkWrapper.NodeWrapper fromNodeWrapped = this.wrappedNetwork.getWrappedNode(fromNode);

		// find possible end stops
		TransitRouterNetwork.TransitRouterNetworkNode toNode = this.transitNetwork.getNearestNode(toCoord);
		TransitRouterNetworkWrapper.NodeWrapper toNodeWrapped = this.wrappedNetwork.getWrappedNode(toNode);

		// find routes between start and end stops

		TransitRouterNetworkTravelTimeCost c = new TransitRouterNetworkTravelTimeCost(this.config);
		Dijkstra d = new Dijkstra(this.wrappedNetwork, c, c);
		Path p = d.calcLeastCostPath(fromNodeWrapped, toNodeWrapped, departureTime);
		ArrayList<Id> linkIds = new ArrayList<Id>(p.links.size());
		for (Link l : p.links) {
			linkIds.add(l.getId());
			System.out.println(l.getId().toString());
		}

		// build route
		return linkIds;
	}

	public List<Leg> calcRoute2(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		// find possible start stops
		Collection<TransitRouterNetwork.TransitRouterNetworkNode> fromNodes = this.transitNetwork.getNearestNodes(fromCoord, this.config.searchRadius);
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetwork.TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.transitNetwork.getNearestNodes(fromCoord, distance + this.config.extensionRadius);
		}
		List<InitialNode> wrappedFromNodes = new ArrayList<InitialNode>();
		for (TransitRouterNetwork.TransitRouterNetworkNode node : fromNodes) {
			TransitRouterNetworkWrapper.NodeWrapper wrappedNode = this.wrappedNetwork.getWrappedNode(node);
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.config.beelineWalkSpeed;
			double initialCost = - (initialTime * this.config.marginalUtilityOfTravelTimeWalk);
			wrappedFromNodes.add(new InitialNode(wrappedNode, initialCost, initialTime + departureTime));
		}

		// find possible end stops
		Collection<TransitRouterNetwork.TransitRouterNetworkNode> toNodes = this.transitNetwork.getNearestNodes(toCoord, this.config.searchRadius);
		if (toNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetwork.TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(toCoord);
			double distance = CoordUtils.calcDistance(toCoord, nearestNode.stop.getStopFacility().getCoord());
			toNodes = this.transitNetwork.getNearestNodes(toCoord, distance + this.config.extensionRadius);
		}
		List<InitialNode> wrappedToNodes = new ArrayList<InitialNode>();
		for (TransitRouterNetwork.TransitRouterNetworkNode node : toNodes) {
			TransitRouterNetworkWrapper.NodeWrapper wrappedNode = this.wrappedNetwork.getWrappedNode(node);
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.config.beelineWalkSpeed;
			double initialCost = - (initialTime * this.config.marginalUtilityOfTravelTimeWalk);
			wrappedToNodes.add(new InitialNode(wrappedNode, initialCost, initialTime + departureTime));
		}

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes);

		if (p == null) {
			return null;
		}

		// now convert the path back into a series of legs with correct routes
		double time = departureTime;
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = null;

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		Link prevLink = null;
		int transitLegCnt = 0;
		for (Link link : p.links) {
			TransitRouterNetworkWrapper.LinkWrapper wrappedLink = (TransitRouterNetworkWrapper.LinkWrapper) link;
			Tuple<TransitLine, TransitRoute> lineData = this.linkMappings.get(wrappedLink.link);
			//			TransitStopFacility nodeData = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getToNode()).node);
			if (lineData == null) {
				TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getFromNode()).node);
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset = (wrappedLink.link.fromNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? wrappedLink.link.fromNode.stop.getArrivalOffset() : wrappedLink.link.fromNode.stop.getDepartureOffset();
					double arrivalTime = getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					transitLegCnt++;
				}
				accessStop = egressStop;
				line = null;
				route = null;
				transitRouteStart = null;
			} else {
				if (lineData.getSecond() != route) {
					// the line changed
					TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getFromNode()).node);
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkWrapper.LinkWrapper) link).link.fromNode.stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.walk);
								double walkTime = CoordUtils.calcDistance(accessStop.getCoord(), egressStop.getCoord()) / this.config.beelineWalkSpeed;
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							} else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.walk);
								double walkTime = CoordUtils.calcDistance(fromCoord, egressStop.getCoord()) / this.config.beelineWalkSpeed;
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							}
						}
					}
					line = lineData.getFirst();
					route = lineData.getSecond();
					accessStop = egressStop;
				}
			}
			prevLink = link;
		}
		if (route != null) {
			TransitRouterNetworkWrapper.LinkWrapper wrappedLink = (TransitRouterNetworkWrapper.LinkWrapper) prevLink;
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)prevLink.getToNode()).node);
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			double arrivalOffset = (wrappedLink.link.toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? wrappedLink.link.toNode.stop.getArrivalOffset() : wrappedLink.link.toNode.stop.getDepartureOffset();
			double arrivalTime = getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);

			legs.add(leg);
			transitLegCnt++;
			accessStop = egressStop;
		}
		if (prevLink != null) {
			leg = new LegImpl(TransportMode.walk);
			double walkTime = CoordUtils.calcDistance(accessStop.getCoord(), toCoord) / this.config.beelineWalkSpeed;
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			leg = new LegImpl(TransportMode.walk);
			double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) / this.config.beelineWalkSpeed;
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		return legs;
	}

	private TransitRouterNetwork buildNetwork() {
		final TransitRouterNetwork network = new TransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					this.nodeMappings.put(node, stop.getStopFacility());
					if (prevNode != null) {
						TransitRouterNetworkLink link = network.createLink(prevNode, node, route, line);
						this.linkMappings.put(link, new Tuple<TransitLine, TransitRoute>(line, route));
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...

		// connect all stops with walking links if they're located less than 100m from each other
		for (TransitRouterNetworkNode node : network.getNodes()) {
			for (TransitRouterNetworkNode node2 : network.getNearestNodes(node.stop.getStopFacility().getCoord(), 100)) {
				if (node != node2) {
					network.createLink(node, node2, null, null); // not sure if null is correct here
				}
			}
		}

		return network;
	}

	/*package*/ TransitRouterNetworkWrapper getWrappedTransitRouterNetwork() {
		return this.wrappedNetwork;
	}

	private double getNextDepartureTime(final TransitRoute route, final TransitRouteStop stop, final double time) {
		// TODO [MR] move this to some helper class
		final double MIDNIGHT = 24.0*3600;

		double earliestDepartureTime = time - stop.getDepartureOffset();
		if (earliestDepartureTime >= MIDNIGHT) {
			earliestDepartureTime = earliestDepartureTime % MIDNIGHT;
		}
		double bestDepartureTime = Double.POSITIVE_INFINITY;
		for (Departure dep : route.getDepartures().values()) {
			// TODO [MR] replace linear search with something faster
			double depTime = dep.getDepartureTime();
			if (depTime >= MIDNIGHT) {
				depTime = depTime % MIDNIGHT;
			}
			if (depTime >= earliestDepartureTime && depTime < bestDepartureTime) {
				bestDepartureTime = depTime;
			}
		}
		if (bestDepartureTime == Double.POSITIVE_INFINITY) {
			// okay, seems we didn't find anything usable, so take the first one in the morning
			for (Departure dep : route.getDepartures().values()) {
				double depTime = dep.getDepartureTime();
				if (depTime >= MIDNIGHT) {
					depTime = depTime % MIDNIGHT;
				}
				if (depTime < bestDepartureTime) {
					bestDepartureTime = depTime;
				}
			}
		}

		while (bestDepartureTime < time) {
			bestDepartureTime += MIDNIGHT;
		}

		return bestDepartureTime;
	}

	/*package*/ void getNextDeparturesAtStop(final TransitStopFacility stop, final double time) {
		Collection<TransitRouterNetworkNode> nodes = this.transitNetwork.getNearestNodes(stop.getCoord(), 0);
		for (TransitRouterNetworkNode node : nodes) {
			double depDelay = node.stop.getDepartureOffset();
			double routeStartTime = time - depDelay;
			double diff = Double.POSITIVE_INFINITY;
			Departure bestDeparture = null;
			for (Departure departure : node.route.getDepartures().values()) {
				if (routeStartTime <= (departure.getDepartureTime()) && ((departure.getDepartureTime() - routeStartTime) < diff)) {
					bestDeparture = departure;
					diff = departure.getDepartureTime() - routeStartTime;
				}
			}
			if (bestDeparture == null) {
				System.out.println("Line: " + node.line.getId().toString()
					+ "  Route: " + node.route.getId()
					+ "  NO DEPARTURE FOUND!");
			} else {
				System.out.println("Line: " + node.line.getId().toString()
						+ "  Route: " + node.route.getId()
						+ "  Departure at: " + Time.writeTime(bestDeparture.getDepartureTime() + depDelay)
						+ "  Waiting time: " + Time.writeTime(diff));
			}
		}
	}

}
