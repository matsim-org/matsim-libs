/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieTransitRouter.java
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
package playground.thibautd.herbie;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dirty implementation of a transit router using the hebie assumptions
 * for walk cost (mainly a modified copy of the default impl)
 * @author thibautd
 */
public class HerbieTransitRouter implements TransitRouter {

	private final TransitRouterNetwork transitNetwork;

	private final MultiNodeDijkstra dijkstra;
	private final TransitRouterConfig config;
	private final TransitTravelDisutility travelDisutility;
	private final TravelTime travelTime;
	
	private final PreparedTransitSchedule data = new PreparedTransitSchedule();

	public HerbieTransitRouter(
			final TransitRouterConfig config,
			final TransitRouterNetwork routerNetwork,
			final TravelTime travelTime,
			final TransitTravelDisutility travelDisutility) {
		this.config = config;
		this.transitNetwork = routerNetwork;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.travelDisutility, this.travelTime);
	}

	@Override
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {

		// find possible start stops
		Collection<TransitRouterNetworkNode> fromNodes = this.transitNetwork.getNearestNodes(fromCoord, this.config.getSearchRadius());
		if (fromNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(fromCoord);
			double distance = CoordUtils.calcEuclideanDistance(fromCoord, nearestNode.stop.getStopFacility().getCoord());
			fromNodes = this.transitNetwork.getNearestNodes(fromCoord, distance + this.config.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			double distance = CoordUtils.calcEuclideanDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.config.getBeelineWalkSpeed();
			double initialCost = HerbieRoutingWalkCostEstimator.getTransitRouterInitialWalkCost( config , initialTime );
			wrappedFromNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find possible end stops
		Collection<TransitRouterNetworkNode> toNodes = this.transitNetwork.getNearestNodes(toCoord, this.config.getSearchRadius());
		if (toNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(toCoord);
			double distance = CoordUtils.calcEuclideanDistance(toCoord, nearestNode.stop.getStopFacility().getCoord());
			toNodes = this.transitNetwork.getNearestNodes(toCoord, distance + this.config.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {
			double distance = CoordUtils.calcEuclideanDistance(toCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / this.config.getBeelineWalkSpeed();
			double initialCost = HerbieRoutingWalkCostEstimator.getTransitRouterInitialWalkCost( config , initialTime );
			wrappedToNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);

		if (p == null) {
			return null;
		}

		double directDist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
		double directWalkTime = directDist / this.config.getBeelineWalkSpeed();
		double directWalkCost = HerbieRoutingWalkCostEstimator.getFormerWalkCostNoDistance( config , directWalkTime );

		double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
		if (directWalkCost < pathCost) {
			List<Leg> legs = new ArrayList<Leg>();
			Leg leg = new LegImpl(TransportMode.transit_walk);
			Route walkRoute = new GenericRouteImpl(null, null);
			leg.setRoute(walkRoute);
			leg.setTravelTime(directWalkTime);
			legs.add(leg);
			return legs;
		}

		return convert( departureTime, p, fromCoord, toCoord ) ;
	}

	protected List<Leg> convert( double departureTime, Path p, Coord fromCoord, Coord toCoord ) {
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
			if (l.getLine() == null) {
				TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset = (((TransitRouterNetworkLink) link).getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? ((TransitRouterNetworkLink) link).fromNode.stop.getArrivalOffset() : ((TransitRouterNetworkLink) link).fromNode.stop.getDepartureOffset();
					double arrivalTime = this.data.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
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
				if (l.getRoute() != route) {
					// the line changed
					TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkLink) link).getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcEuclideanDistance(accessStop.getCoord(), egressStop.getCoord()) / this.config.getBeelineWalkSpeed();
								Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
								leg.setRoute(walkRoute);
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							} else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcEuclideanDistance(fromCoord, egressStop.getCoord()) / this.config.getBeelineWalkSpeed();
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							}
						}
					}
					line = l.getLine();
					route = l.getRoute();
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
			double arrivalTime = this.data.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);

			legs.add(leg);
			transitLegCnt++;
			accessStop = egressStop;
		}
		if (prevLink != null) {
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime;
			if (accessStop == null) {
				walkTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / this.config.getBeelineWalkSpeed();
			} else {
				walkTime = CoordUtils.calcEuclideanDistance(accessStop.getCoord(), toCoord) / this.config.getBeelineWalkSpeed();
			}
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / this.config.getBeelineWalkSpeed();
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

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
	}

}
